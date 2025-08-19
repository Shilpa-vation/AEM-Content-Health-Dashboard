package com.chd.core.servlets;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.chd.core.config.CHDConfig;
import com.chd.core.service.CHDAssetService;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationQueue;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import javax.jcr.Session;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class, property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/content-health-audit"
})
@Designate(ocd = CHDConfig.class)
public class ContentHealthAuditServlet extends SlingAllMethodsServlet {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private AgentManager agentManager;

    CHDAssetService chdAssetService = new CHDAssetService();

    private static final Logger logger = LoggerFactory.getLogger(ContentHealthAuditServlet.class);

    private String ROOT_PATH;
    private String DAM_ROOT_PATH;
    private static final long STALE_DAYS = 180;
    private int UNPUBLISHED_PAGES_COUNT;
    private int TOTAL_PAGES_COUNT;
    private int ISSUE_COUNT;
    private long recentPageCount = 0;
    private long recentAssetCount = 0;
    public String configRootPath;
    public String configdamPath;

    @Activate
    @Modified
    protected void activate(CHDConfig config) {
        configRootPath = config.contentRootPath();
        configdamPath = config.assetRootPath();
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String format = request.getParameter("format");

        ResourceResolver resolver = request.getResourceResolver();
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> pages = new ArrayList<>();
        List<Map<String, Object>> widges = new ArrayList<>();
        Map<String, Object> assetAnalysis = new HashMap<>();
        String formattedDate = StringUtils.EMPTY;
        Session session = resolver.adaptTo(Session.class);
        initializaVar(request);
        assetAnalysis = chdAssetService.analyzeAssets(resolver, DAM_ROOT_PATH, ROOT_PATH);
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
                    logger.info("Inside Page Name :", page.getPath());
                    Resource content = page.getContentResource();
                    if (content == null)
                        continue;
                    ValueMap props = content.getValueMap();
                    List<Map<String, String>> issues = new ArrayList<>();
                    String path = page.getPath();
                    String pageTitle = page.getTitle();
                    Calendar modifiedDate = props.get("cq:lastModified", Calendar.class);
                    Calendar createdDate = props.get("jcr:created", Calendar.class);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    formattedDate = modifiedDate != null ? sdf.format(modifiedDate.getTime())
                            : sdf.format(createdDate.getTime());
                    checkMetadata(props, issues);
                    checkSEO(props, issues);
                    checkAudit(props, issues);
                    checkLinks(content, httpClient, resolver, issues);
                    checkWorkflow(resolver, content, issues);
                    Map<String, Object> pageReport = new HashMap<>();
                    pageReport.put("path", path);
                    pageReport.put("title", pageTitle);
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
        widgetReport.put("issueCount", ISSUE_COUNT);
        widgetReport.put("replicationQItems", getQueueCount());
        widgetReport.put("activeWorkflowCount", getActiveWorkflowCount(resolver));
        try {
            widgetReport.put("recentPageCount",
                    chdAssetService.getRecentPageCount(session, recentPageCount, ROOT_PATH));
            widgetReport.put("recentAssetCount",
                    chdAssetService.getRecentAssetCount(session, recentAssetCount, DAM_ROOT_PATH));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        widges.add(widgetReport);
        report.put("site", ROOT_PATH);
        report.put("damSitePath", DAM_ROOT_PATH);
        report.put("generatedAt", Instant.now().toString());
        report.put("widges", widges);
        report.put("pages", pages);
        report.put("assets", assetAnalysis);
        if ("excel".equalsIgnoreCase(format)) {
            generateExcelReport(report, response);
        } else {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(report));
        }
    }

    private void initializaVar(SlingHttpServletRequest request) {
        UNPUBLISHED_PAGES_COUNT = 0;
        TOTAL_PAGES_COUNT = 0;
        ISSUE_COUNT = 0;
        this.ROOT_PATH = isNullOrEmpty(request.getParameter("value1")) ? configRootPath
                : request.getParameter("value1");
        this.DAM_ROOT_PATH = isNullOrEmpty(request.getParameter("value2")) ? configdamPath
                : request.getParameter("value2");

    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void checkMetadata(ValueMap props, List<Map<String, String>> issues) {
        if (StringUtils.isBlank(props.get("jcr:title", "")))
            addIssue(issues, "metadata", "Missing title", "WARN");
        if (StringUtils.isBlank(props.get("jcr:description", "")))
            addIssue(issues, "metadata", "Missing description", "WARN");
        if (props.get("cq:tags", new String[] {}).length == 0)
            addIssue(issues, "metadata", "Missing tags", "WARN");
        if (StringUtils.isBlank(props.get("language", "")))
            addIssue(issues, "metadata", "Missing language", "LOW");
    }

    private void checkSEO(ValueMap props, List<Map<String, String>> issues) {
        if (StringUtils.isBlank(props.get("canonicalUrl", "")))
            addIssue(issues, "seo", "Missing canonical URL", "WARN");
        if (StringUtils.isBlank(props.get("robots", "")))
            addIssue(issues, "seo", "Missing robots meta tag", "LOW");
        if (StringUtils.isBlank(props.get("og:title", "")))
            addIssue(issues, "seo", "Missing Open Graph title", "LOW");
        if (StringUtils.isBlank(props.get("twitter:title", "")))
            addIssue(issues, "seo", "Missing Twitter Card title", "LOW");
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

        if (resource == null)
            return;

        // Check current resource's properties
        ValueMap props = resource.getValueMap();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String url = (String) value;
                if ((key.contains("href") || key.contains("src") || key.contains("link")
                        || key.contains("fileReference"))) {
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

    private void checkWorkflow(ResourceResolver resolver, Resource content, List<Map<String, String>> issues) {
        try {
            WorkflowSession wfSession = resolver.adaptTo(WorkflowSession.class);
            if (wfSession == null)
                return;

            String payloadPath = content.getPath().replaceAll("/jcr:content", "");
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

    public int getQueueCount() {
        Agent agent = agentManager.getAgents().get("publish");
        if (agent != null && agent.isEnabled() && agent.isValid()) {
            ReplicationQueue queue = agent.getQueue();
            return queue.entries().size();
        }
        return 0; // Agent not found or invalid
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

    private void addIssue(List<Map<String, String>> issues, String type, String message, String status) {
        Map<String, String> issue = new HashMap<>();
        ISSUE_COUNT++;
        issue.put("type", type);
        issue.put("message", message);
        issue.put("status", status);
        issues.add(issue);
    }
    
    private void generateExcelReport(Map<String, Object> report, SlingHttpServletResponse response) throws IOException {
    Workbook workbook = new XSSFWorkbook();

    // Summary Sheet
    Sheet summarySheet = workbook.createSheet("Summary");
    int rowIdx = 0;
    for (Map.Entry<String, Object> entry : report.entrySet()) {
        if (entry.getValue() instanceof List || entry.getValue() instanceof Map) continue;
        Row row = summarySheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(entry.getKey());
        row.createCell(1).setCellValue(String.valueOf(entry.getValue()));
    }

    // Pages Sheet
    Sheet pagesSheet = workbook.createSheet("Pages");
    List<Map<String, Object>> pages = (List<Map<String, Object>>) report.get("pages");
    if (pages != null && !pages.isEmpty()) {
        Row header = pagesSheet.createRow(0);
        header.createCell(0).setCellValue("Path");
        header.createCell(1).setCellValue("Title");
        header.createCell(2).setCellValue("Last Modified");
        header.createCell(3).setCellValue("Issue Count");
        header.createCell(4).setCellValue("Issue Descriptions");

        int i = 1;
        for (Map<String, Object> page : pages) {
            Row row = pagesSheet.createRow(i++);
            row.createCell(0).setCellValue(String.valueOf(page.get("path")));
            row.createCell(1).setCellValue(String.valueOf(page.get("title")));
            row.createCell(2).setCellValue(String.valueOf(page.get("lastModified")));
            List<Map<String, String>> issues = (List<Map<String, String>>) page.get("issues");
            row.createCell(3).setCellValue(issues != null ? issues.size() : 0);
            // Collect issue descriptions
            String issueSummary = "";
            if (issues != null && !issues.isEmpty()) {
                List<String> descriptions = new ArrayList<>();
                for (Map<String, String> issue : issues) {
                    String desc = issue.get("message");
                    if (desc != null) {
                        descriptions.add(desc);
                    }
                }
                issueSummary = String.join(", ", descriptions);
            }
            row.createCell(4).setCellValue(issueSummary);

            }
    }

    // Existing Assets Sheet
Sheet assetSheet = workbook.createSheet("Assets");
Map<String, Object> assets = (Map<String, Object>) report.get("assets");

if (assets != null) {
    int r = 0;
    for (Map.Entry<String, Object> entry : assets.entrySet()) {
        Row row = assetSheet.createRow(r++);
        row.createCell(0).setCellValue(entry.getKey());
        row.createCell(1).setCellValue(String.valueOf(entry.getValue()));
    }

    // Create Asset Issues Sheet
    Sheet assetIssuesSheet = workbook.createSheet("Asset Issues");
    Row header = assetIssuesSheet.createRow(0);
    header.createCell(0).setCellValue("Path");
    header.createCell(1).setCellValue("Title");
    header.createCell(2).setCellValue("Status");
    header.createCell(3).setCellValue("Messages");

    int i = 1;
    List<Map<String, Object>> issues = (List<Map<String, Object>>) assets.get("issues");
    if (issues != null) {
        for (Map<String, Object> issue : issues) {
            if ("assets".equals(issue.get("type"))) {
                Row row = assetIssuesSheet.createRow(i++);
                row.createCell(0).setCellValue(String.valueOf(issue.get("path")));
                row.createCell(1).setCellValue(String.valueOf(issue.get("title")));
                row.createCell(2).setCellValue(String.valueOf(issue.get("status")));

                List<String> messages = (List<String>) issue.get("messages");
                String messageSummary = messages != null ? String.join(", ", messages) : "";
                row.createCell(3).setCellValue(messageSummary);
            }
        }
    }
}

    

    // Widgets Sheet
    Sheet widgetSheet = workbook.createSheet("Widgets");
    List<Map<String, Object>> widgets = (List<Map<String, Object>>) report.get("widges");
    if (widgets != null && !widgets.isEmpty()) {
        int r = 0;
        for (Map<String, Object> widget : widgets) {
            for (Map.Entry<String, Object> entry : widget.entrySet()) {
                Row row = widgetSheet.createRow(r++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(String.valueOf(entry.getValue()));
            }
        }
    }

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=report.xlsx");

    try (ServletOutputStream out = response.getOutputStream()) {
        workbook.write(out);
        workbook.close();
    }
}

}