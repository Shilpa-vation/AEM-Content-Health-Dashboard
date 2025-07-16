package com.chd.core.servlets;
 
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationQueue;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.workflow.status.WorkflowStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/content-health-audit"
    }
)
public class ContentHealthAuditServlet extends SlingAllMethodsServlet {
 
    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private AgentManager agentManager;   
 
    private static final Logger logger = LoggerFactory.getLogger(ContentHealthAuditServlet.class);
 
    private static final String ROOT_PATH = "/content/chd/us/en/my-account";
    private static final String DAM_ROOT_PATH = "/content/dam";
    private static final long STALE_DAYS = 180;
    private static final long MAX_IMAGE_SIZE = 2_000_000;
    private int UNPUBLISHED_PAGES_COUNT;
    private int TOTAL_PAGES_COUNT;
    private int ISSUE_COUNT;
    private long recentPageCount = 0;
    private long recentAssetCount = 0;
 
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
 
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        ResourceResolver resolver = request.getResourceResolver();
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> pages = new ArrayList<>();
        List<Map<String, Object>> widges = new ArrayList<>();
        Map<String, Object> assetAnalysis = new HashMap<>();
        String formattedDate = StringUtils.EMPTY;
        Runtime runtime = Runtime.getRuntime();
        Session session = resolver.adaptTo(Session.class);
        initializaVar();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            logger.info("Inside resolver");
            PageManager pageManager = resolver.adaptTo(PageManager.class);
           Page root = pageManager.getPage(ROOT_PATH);
 
            if (root != null) {
                Iterator<Page> pageIterator = root.listChildren(null, true);
                while (pageIterator.hasNext()) {
                    TOTAL_PAGES_COUNT++;
                    logger.info("Inside Page iterator :");
                    Page page = pageIterator.next();
                    logger.info("Inside Page Name :",page.getPath());
                    Resource content = page.getContentResource();
                    if (content == null) continue;
                    ValueMap props = content.getValueMap();
                    List<Map<String, String>> issues = new ArrayList<>();
                    String path = page.getPath();
                    String pageTitle = page.getTitle();
                    Calendar modifiedDate = props.get("cq:lastModified", Calendar.class);
                    Calendar createdDate = props.get("jcr:created", Calendar.class);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    formattedDate = modifiedDate != null ? sdf.format(modifiedDate.getTime()) : sdf.format(createdDate.getTime());
                    checkMetadata(props, issues);
                    checkSEO(props, issues);
                    checkAudit(props, issues);
                    checkLinks(content, httpClient, resolver, issues);
                    checkWorkflow(resolver,content, issues);
                    assetAnalysis = analyzeAssets(resolver);
 
                    getRecentPageCount(session,recentPageCount,recentAssetCount);
                    Map<String, Object> pageReport = new HashMap<>();
                    pageReport.put("path", path);
                    pageReport.put("title",pageTitle);
                    pageReport.put("lastModified", formattedDate);
                    pageReport.put("issues", issues);
                    pages.add(pageReport);
                }
            }
 
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
            return;
        }
        Map<String, Object> widgetReport = new HashMap<>();
        widgetReport.put("totalPages", TOTAL_PAGES_COUNT);
        widgetReport.put("unpublishedPages", UNPUBLISHED_PAGES_COUNT);
        widgetReport.put("publishedPages", (TOTAL_PAGES_COUNT - UNPUBLISHED_PAGES_COUNT));
        widgetReport.put("issueCount",ISSUE_COUNT);
        widgetReport.put("replicationQItems",getQueueCount());
        widgetReport.put("activeWorkflowCount",getActiveWorkflowCount(resolver));
        widgetReport.put("maxHeapSize",runtime.maxMemory()/(1024*1024) +" MB");         // Maximum heap size (-Xmx)
        widgetReport.put( "totalHeapSize",runtime.totalMemory()/(1024*1024) +" MB");     // Current allocated heap
        widgetReport.put("freeHeapSize",runtime.freeMemory()/(1024*1024) +" MB"); 
        widgetReport.put("recentPageCount", recentPageCount);
        widgetReport.put("recentAssetCount", recentAssetCount);
        widges.add(widgetReport);
        report.put("site", ROOT_PATH);
        report.put("damSitePath",DAM_ROOT_PATH);
        report.put("generatedAt", Instant.now().toString());
        report.put("widges", widges);
        report.put("pages", pages);
        report.put("assets", assetAnalysis);
 
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(report));
    }

    private void initializaVar(){
        UNPUBLISHED_PAGES_COUNT = 0;
        TOTAL_PAGES_COUNT = 0;
        ISSUE_COUNT = 0;
    }
 
    private void checkMetadata(ValueMap props, List<Map<String, String>> issues) {
        if (StringUtils.isBlank(props.get("jcr:title", ""))) addIssue(issues, "metadata", "Missing title", "WARN");
        if (StringUtils.isBlank(props.get("jcr:description", ""))) addIssue(issues, "metadata", "Missing description", "WARN");
        if (props.get("cq:tags", new String[]{}).length == 0) addIssue(issues, "metadata", "Missing tags", "WARN");
        if (StringUtils.isBlank(props.get("language", ""))) addIssue(issues, "metadata", "Missing language", "LOW");
    }
 
    private void checkSEO(ValueMap props, List<Map<String, String>> issues) {
        if (StringUtils.isBlank(props.get("canonicalUrl", ""))) addIssue(issues, "seo", "Missing canonical URL", "WARN");
        if (StringUtils.isBlank(props.get("robots", ""))) addIssue(issues, "seo", "Missing robots meta tag", "LOW");
        if (StringUtils.isBlank(props.get("og:title", ""))) addIssue(issues, "seo", "Missing Open Graph title", "LOW");
        if (StringUtils.isBlank(props.get("twitter:title", ""))) addIssue(issues, "seo", "Missing Twitter Card title", "LOW");
    }
 
    private void checkAudit(ValueMap props, List<Map<String, String>> issues) {
        Calendar lastModified = props.get("cq:lastModified", Calendar.class);
        if (lastModified != null) {
            Instant lastMod = lastModified.toInstant();
            if (lastMod.isBefore(Instant.now().minus(STALE_DAYS, ChronoUnit.DAYS))) {
                addIssue(issues, "audit", "Stale content (not updated in 6+ months)", "WARN");
            }
        }
        String replication = props.get("cq:lastReplicationAction", "");
        if (!"Activate".equals(replication)) {
            UNPUBLISHED_PAGES_COUNT++;
            addIssue(issues, "audit", "Page not published", "WARN");
        }
    }
 
   private void checkLinks(Resource resource, CloseableHttpClient httpClient,
                                 ResourceResolver resolver, List<Map<String, String>> issues) {

        if (resource == null) return;

        // Check current resource's properties
        ValueMap props = resource.getValueMap();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String url = (String) value;
                if ((key.contains("href") || key.contains("src") || key.contains("link") || key.contains("fileReference"))) {
                    if (url.startsWith("http")) {
                        try {
                            HttpHead request = new HttpHead(url);
                            int status = httpClient.execute(request).getStatusLine().getStatusCode();
                            if (status >= 400) {
                                addIssue(issues, "links", "Broken external link: " + url, "ERROR");
                            }
                        } catch (Exception e) {
                            addIssue(issues, "links", "Broken link (exception): " + url, "ERROR");
                        }
                    } else if (url.startsWith("/content") && resolver.getResource(url) == null) {
                        addIssue(issues, "links", "Broken internal reference: " + url, "ERROR");
                    }
                }
            }
        }

        // Recurse into children
        for (Resource child : resource.getChildren()) {
            checkLinks(child, httpClient, resolver, issues);
        }
    }

 
    private void checkWorkflow(ResourceResolver resolver,Resource content, List<Map<String, String>> issues) {
    try {
        WorkflowSession wfSession = resolver.adaptTo(WorkflowSession.class);
        if (wfSession == null) return;
 
        String payloadPath = content.getPath().replaceAll("/jcr:content","");
       WorkItem[] activeWorkflows = wfSession.getActiveWorkItems();
 
        for (WorkItem wf : activeWorkflows) {
            WorkflowData data = wf.getWorkflowData();
            if (data.getPayload().toString().equals(payloadPath)) {
                addIssue(issues, "workflow", "Page is in workflow", "ERROR");
                break;
            }
        }
    } catch (Exception e) {
        logger.error("Error checking workflow status for: {}", content.getPath(), e);
    }
    }
 
    private Map<String, Object> analyzeAssets(ResourceResolver resolver) {
        Map<String, Object> assetReport = new HashMap<>();
        Map<String, Map<String, Object>> assetIssueMap = new HashMap<>();
        Map<String, Integer> formatCounts = new HashMap<>();
        Set<String> seenNames = new HashSet<>();
        List<Asset> staleAssets = new ArrayList<>();
    
        Resource damRoot = resolver.getResource(DAM_ROOT_PATH);
        if (damRoot == null) return assetReport;
    
        traverseAssets(damRoot, assetIssueMap, formatCounts, seenNames);
        findUnreferencedAssets(resolver.getResource(DAM_ROOT_PATH),resolver,staleAssets);

        List<Map<String, Object>> filteredIssues = new ArrayList<>();
        for (Map<String, Object> entry : assetIssueMap.values()) {
            List<String> messages = (List<String>) entry.get("messages");
            if (messages != null && !messages.isEmpty()) {
                filteredIssues.add(entry);
            }
        }
        assetReport.put("issues", filteredIssues);
        assetReport.put("issueCount", filteredIssues.size());
        assetReport.put("formatCounts", formatCounts);
        assetReport.put("totalAssets", seenNames.size());
        assetReport.put("staleAssets", staleAssets.size());
    
        return assetReport;
    }
 
    private void traverseAssets(Resource resource,
                            Map<String, Map<String, Object>> assetIssueMap,
                            Map<String, Integer> formatCounts,
                            Set<String> seenNames) {
 
        for (Resource child : resource.getChildren()) {
            Asset asset = child.adaptTo(Asset.class);
            if (asset != null) {
                String name = asset.getName();
                String path = asset.getPath();
                String mimeType = asset.getMimeType();
                Rendition original = asset.getRendition("original");
                String alt = asset.getMetadataValue("dc:description");
    
                // Set up issue entry if not already present
                Map<String, Object> issueEntry = assetIssueMap.computeIfAbsent(path, p -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("path", p);
                    entry.put("title",name);
                    entry.put("type", "assets");
                    entry.put("messages", new ArrayList<String>());
                    entry.put("status", "WARN"); // Can be upgraded to dynamic severity later
                    return entry;
                });
    
                List<String> messages = (List<String>) issueEntry.get("messages");
    
                // Check for large asset
                if (original != null) {
                    long size = original.getSize();
                    if (size > MAX_IMAGE_SIZE) {
                        messages.add("Large asset (" + (size / 1024) + " KB)");
                    }
                }
    
                // Format count
                String format = mimeType != null ? mimeType.substring(mimeType.lastIndexOf("/") + 1) : "unknown";
                formatCounts.merge(format.toLowerCase(), 1, Integer::sum);
    
                // Missing alt text
                if (StringUtils.isBlank(alt)) {
                    messages.add("Missing alt text");
                }
    
                // Duplicate asset name
                if (!seenNames.add(name)) {
                    messages.add("Duplicate asset name: " + name);
                }
    
            } else {
                // Recurse into folder
                traverseAssets(child, assetIssueMap, formatCounts, seenNames);
            }
        }
    }

    public void findUnreferencedAssets(Resource damRoot,ResourceResolver resolver,List<Asset> staleAssets) {
        for (Resource assetRes : damRoot.getChildren()) {
            Asset asset = assetRes.adaptTo(Asset.class);
            if (asset != null) {
                AssetReferenceSearch search = new AssetReferenceSearch(assetRes.adaptTo(Node.class), ROOT_PATH, resolver);
                Map<String, Asset> refs = search.search();
                if (refs.isEmpty()) {
                    staleAssets.add(asset);
                }
            } else {
                // Recurse into folder
                findUnreferencedAssets(assetRes,resolver,staleAssets);
            }
        }
    }

    
    public int getQueueCount() {
        Agent agent = agentManager.getAgents().get("publish");
        if (agent != null && agent.isEnabled() && agent.isValid()) {
            ReplicationQueue queue = agent.getQueue();
            return queue.entries().size();
        }
        return -1; // Agent not found or invalid
    }

    public int getActiveWorkflowCount(ResourceResolver resolver) {
    WorkflowSession wfSession = resolver.adaptTo(WorkflowSession.class);
    if (wfSession == null) {
        logger.warn("WorkflowSession could not be adapted");
        return 0;
    }

    WorkItem[] activeWorkflows = null;
    try {
        activeWorkflows = wfSession.getActiveWorkItems();
    } catch (WorkflowException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    return activeWorkflows.length;
}

    
    public int getFailedWorkflowCount() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.adobe.granite.workflow:type=Maintenance");

            // Pass null to get failures across all models
            Object[] params = { null };
            String[] signature = { "java.lang.String" };

            Object result = mbs.invoke(name, "returnFailedWorkflowCount", params, signature);
            return Integer.parseInt(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void getRecentPageCount(Session session,long recentPageCount,long recentAssetCount) throws Exception {
        QueryManager qm = session.getWorkspace().getQueryManager();

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.HOUR, -24);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String dateStr = sdf.format(yesterday.getTime());

        String pageQueryStr = "SELECT * FROM [cq:PageContent] AS page " +
                        "WHERE page.[jcr:created] >= CAST('" + dateStr + "' AS DATE) " +
                        "AND ISDESCENDANTNODE(page, '/content/chd')";

        Query pageQuery = qm.createQuery(pageQueryStr, Query.JCR_SQL2);
        QueryResult pageResult = pageQuery.execute();
        RowIterator pageRows = pageResult.getRows();
        // String assetQueryStr = "SELECT * FROM [dam:AssetContent] AS asset " +
        //               "WHERE asset.[jcr:created] >= CAST('" + dateStr + "' AS DATE) " +
        //               "AND ISDESCENDANTNODE(asset, '/content/dam/chd')";

        // Query assetQuery = qm.createQuery(assetQueryStr, Query.JCR_SQL2);
        // QueryResult assResult = assetQuery.execute();
        // RowIterator assRows = assResult.getRows();
        recentPageCount = pageRows.getSize();
        // recentAssetCount = assRows.getSize();
    }

    private void addIssue(List<Map<String, String>> issues, String type, String message, String status) {
        Map<String, String> issue = new HashMap<>();
        ISSUE_COUNT++;
        issue.put("type", type);
        issue.put("message", message);
        issue.put("status", status);
        issues.add(issue);
    }
}