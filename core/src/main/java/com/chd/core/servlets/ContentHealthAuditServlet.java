package com.chd.core.servlets;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
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

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

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


    private static final Logger logger = LoggerFactory.getLogger(ContentHealthAuditServlet.class);

    private static final String ROOT_PATH = "/content/chd/us/en/products";
    private static final String DAM_ROOT_PATH = "/content/dam/chd";
    private static final long STALE_DAYS = 180;
    private static final long MAX_IMAGE_SIZE = 2_000_000;
    private int UNPUBLISHED_PAGES_COUNT = 0;
    private int TOTAL_PAGES_COUNT = 0;
    private int PUBLISHED_PAGES_COUNT = 0;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> pages = new ArrayList<>();
        Map<String, Object> assetAnalysis = new HashMap<>();
        Map<String, Object> param = new HashMap<String, Object>();
        param.put(ResourceResolverFactory.SUBSERVICE, "chdCodeSystemUser");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ResourceResolver resolver = request.getResourceResolver();
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

                    checkMetadata(props, issues);
                    checkSEO(props, issues);
                    checkAudit(props, issues);
                    checkLinks(content, httpClient, resolver, issues);
                    checkWorkflow(resolver,content, issues);
                    assetAnalysis = analyzeAssets(resolver);


                    Map<String, Object> pageReport = new HashMap<>();
                    pageReport.put("path", path);
                    pageReport.put("issues", issues);
                    pages.add(pageReport);
                }
            }

        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
            return;
        }

        report.put("site", ROOT_PATH);
        report.put("damSitePath",DAM_ROOT_PATH);
        report.put("generatedAt", Instant.now().toString());
        report.put("totalPages", TOTAL_PAGES_COUNT);
        report.put("unpublishedPages", UNPUBLISHED_PAGES_COUNT);
        report.put("publishedPages", (TOTAL_PAGES_COUNT - UNPUBLISHED_PAGES_COUNT));
        report.put("pages", pages);
        report.put("assets", assetAnalysis);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(report));
    }

    private void checkMetadata(ValueMap props, List<Map<String, String>> issues) {
        if (StringUtils.isBlank(props.get("jcr:title", ""))) addIssue(issues, "metadata", "Missing title", "WARN");
        if (StringUtils.isBlank(props.get("jcr:description", ""))) addIssue(issues, "metadata", "Missing description", "WARN");
        if (props.get("cq:tags", new String[]{}).length == 0) addIssue(issues, "metadata", "Missing tags", "WARN");
        if (StringUtils.isBlank(props.get("language", ""))) addIssue(issues, "metadata", "Missing language", "WARN");
    }

    private void checkSEO(ValueMap props, List<Map<String, String>> issues) {
        if (StringUtils.isBlank(props.get("canonicalUrl", ""))) addIssue(issues, "seo", "Missing canonical URL", "WARN");
        if (StringUtils.isBlank(props.get("robots", ""))) addIssue(issues, "seo", "Missing robots meta tag", "WARN");
        if (StringUtils.isBlank(props.get("og:title", ""))) addIssue(issues, "seo", "Missing Open Graph title", "WARN");
        if (StringUtils.isBlank(props.get("twitter:title", ""))) addIssue(issues, "seo", "Missing Twitter Card title", "WARN");
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

    private void checkLinks(Resource content, CloseableHttpClient httpClient, ResourceResolver resolver, List<Map<String, String>> issues) {
        content.getChildren().forEach(comp -> {
            comp.getValueMap().forEach((key, value) -> {
                if (value instanceof String && (key.contains("href") || key.contains("src"))) {
                    String url = (String) value;
                    if (url.startsWith("http")) {
                        try {
                            HttpHead request = new HttpHead(url);
                            int status = httpClient.execute(request).getStatusLine().getStatusCode();
                            if (status >= 400) addIssue(issues, "links", "Broken external link: " + url, "ERROR");
                        } catch (Exception e) {
                            addIssue(issues, "links", "Broken link (exception): " + url, "ERROR");
                        }
                    } else if (url.startsWith("/content") && resolver.getResource(url) == null) {
                        addIssue(issues, "links", "Broken internal reference: " + url, "ERROR");
                    }
                }
            });
        });
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

    Resource damRoot = resolver.getResource(DAM_ROOT_PATH);
    if (damRoot == null) return assetReport;

    traverseAssets(damRoot, assetIssueMap, formatCounts, seenNames);

    assetReport.put("issues", new ArrayList<>(assetIssueMap.values()));
    assetReport.put("formatCounts", formatCounts);
    assetReport.put("totalAssets", seenNames.size());
    assetReport.put("issueCount", assetIssueMap.size());

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

    private void addIssue(List<Map<String, String>> issues, String type, String message, String status) {
        Map<String, String> issue = new HashMap<>();
        issue.put("type", type);
        issue.put("message", message);
        issue.put("status", status);
        issues.add(issue);
    }
}
