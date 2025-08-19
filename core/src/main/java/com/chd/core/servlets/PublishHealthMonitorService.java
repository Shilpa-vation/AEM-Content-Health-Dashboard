package com.chd.core.servlets;

import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Activate;

import javax.jcr.RepositoryException;
import javax.management.MBeanServer;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.lang.management.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Component(service = Servlet.class, property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/content-health-audit-publish"
})
public class PublishHealthMonitorService extends SlingAllMethodsServlet {

    private MBeanServer mBeanServer;
    private OperatingSystemMXBean osBean;
    private MemoryMXBean memoryBean;
    

    @Activate
    protected void activate() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        osBean = ManagementFactory.getOperatingSystemMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        Map<String, Object> statusReport = getPublishHealthStats(request);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(statusReport));

    }

    public Map<String, Object> getPublishHealthStats(SlingHttpServletRequest request) {
        Map<String, Object> stats = new HashMap<>();

        try {
            ResourceResolver resolver = request.getResourceResolver();

            // 🔍 System Errors
            stats.put("MissingTemplates", scanForMissingTemplates("/content", resolver));
            stats.put("BrokenLinks", scanForBrokenLinks("/content", resolver));

            // ⏱️ Response Time Errors
            stats.put("SlowPages", getSlowPagesViaTTFB("/content", 500, resolver));

            // readCriticalErrorsFromSlingLog());

            // 🧠 CPU & Memory
            // cannot get as its from AEM cloud stats.put("CpuUtilization", getCpuLoad());
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            long usedMemMB = heap.getUsed() / (1024 * 1024);
            long maxMemMB = heap.getMax() / (1024 * 1024);
            stats.put("MemoryUsedMB", usedMemMB);
            stats.put("MemoryMaxMB", maxMemMB);
            stats.put("MemoryUsagePercent", maxMemMB > 0 ? (usedMemMB * 100.0) / maxMemMB : 0.0);

            // 💾 Disk Space
            File root = new File("/");
            long freeDiskGB = root.getFreeSpace() / (1024 * 1024 * 1024);
            long totalDiskGB = root.getTotalSpace() / (1024 * 1024 * 1024);
            stats.put("DiskFreeGB", freeDiskGB);
            stats.put("DiskTotalGB", totalDiskGB);
            stats.put("DiskUsagePercent", totalDiskGB > 0 ? ((totalDiskGB - freeDiskGB) * 100.0) / totalDiskGB : 0.0);

            // 🚀 Synthetic Content Performance Metrics
            List<Map<String, Object>> pageMetrics = getSyntheticPageMetrics("/content", 500, request);
            stats.put("SyntheticPageMetrics", pageMetrics);

        } catch (Exception e) {
            stats.put("error", "Monitoring failed: " + e.getMessage());
        }

        return stats;
    }

    private List<Map<String, Object>> getSyntheticPageMetrics(String rootPath, double thresholdMs,
            SlingHttpServletRequest request) {
        List<Map<String, Object>> pageMetrics = new ArrayList<>();

        ResourceResolver resolver = request.getResourceResolver();
        try {
            Resource root = resolver.getResource(rootPath);

            if (root != null) {
                for (Resource page : root.getChildren()) {
                    scanPageSyntheticRecursively(page,pageMetrics,thresholdMs);
                }
            }
        } catch (Exception e) {
            pageMetrics.add(Map.of("error", "Synthetic monitoring failed: " + e.getMessage()));
        }

        return pageMetrics;
    }

    private void scanPageSyntheticRecursively(Resource page,List<Map<String, Object>> pageMetrics,double thresholdMs){
        if ("cq:Page".equals(page.getResourceType())) {
            String pagePath = page.getPath();
            String url = buildPublishUrl(pagePath);

            double ttfb = measureTTFB(url);
            boolean isSlow = ttfb > thresholdMs;

            List<Map<String, Object>> assetMetrics = getAssetDeliveryMetrics(page);

            pageMetrics.add(Map.of(
                    "path", pagePath,
                    "url", url,
                    "TTFBms", ttfb,
                    "isSlow", isSlow,
                    "AssetDelivery", assetMetrics));
        }
        for (Resource child : page.getChildren()) {
            scanPageSyntheticRecursively(child, pageMetrics,thresholdMs);
        }

    }

    private List<Map<String, Object>> getAssetDeliveryMetrics(Resource page) {
        List<Map<String, Object>> assets = new ArrayList<>();
        Resource content = page.getChild("jcr:content");

        if (content != null) {
            for (Resource child : content.getChildren()) {
                ValueMap props = child.getValueMap();
                if (props.containsKey("fileReference")) {
                    String assetPath = props.get("fileReference", String.class);
                    String assetUrl = buildPublishUrl(assetPath);
                    double ttfb = measureTTFB(assetUrl);

                    assets.add(Map.of(
                            "assetPath", assetPath,
                            "TTFBms", ttfb));
                }
            }
        }

        return assets;
    }

    // 🔧 Utility Methods

    private List<String> scanForMissingTemplates(String rootPath, ResourceResolver resolver) {
        List<String> missingTemplates = new ArrayList<>();

        try {
            Resource root = resolver.getResource(rootPath);

            if (root != null) {
                scanPagesRecursively(root, missingTemplates);
            }
        } catch (Exception e) {
            missingTemplates.add("Error scanning for missing templates: " + e.getMessage());
        }

        return missingTemplates;
    }

    private void scanPagesRecursively(Resource resource, List<String> missingTemplates) {
        if ("cq:Page".equals(resource.getResourceType())) {
            Resource content = resource.getChild("jcr:content");
            if (content != null) {
                ValueMap props = content.getValueMap();
                if (!props.containsKey("cq:template")) {
                    missingTemplates.add(resource.getPath() + " → missing cq:template");
                }
            } else {
                missingTemplates.add(resource.getPath() + " → missing jcr:content");
            }
        }

        for (Resource child : resource.getChildren()) {
            scanPagesRecursively(child, missingTemplates);
        }
    }

    private List<String> scanForBrokenLinks(String rootPath, ResourceResolver resolver) {
        List<String> brokenLinks = new ArrayList<>();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Resource root = resolver.getResource(rootPath);

            if (root != null) {
                for (Resource page : root.getChildren()) {
                    Resource content = page.getChild("jcr:content");
                    if (content != null) {
                        for (Resource child : content.getChildren()) {
                            ValueMap props = child.getValueMap();
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
                                                int status = httpClient.execute(request).getStatusLine()
                                                        .getStatusCode();
                                                if (status >= 400) {
                                                    brokenLinks.add(page.getPath() + " → Broken external link: " + url);
                                                }
                                            } catch (Exception e) {
                                                brokenLinks.add(page.getPath() + " → Broken link (exception): " + url);
                                            }
                                        } else if (url.startsWith("/content") && resolver.getResource(url) == null) {
                                            brokenLinks.add(page.getPath() + " → Broken internal reference: " + url);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            brokenLinks.add("Error scanning for broken links: " + e.getMessage());
        }

        return brokenLinks;
    }


    // 🔐 Security and Compliance
    private Map<String, Object> getAccessControlHealth(ResourceResolver resolver) {
        Map<String, Object> accessHealth = new HashMap<>();
        List<String> adminUsers = new ArrayList<>();
        List<String> elevatedGroups = new ArrayList<>();

        try {
            UserManager userManager = resolver.adaptTo(UserManager.class);
            if (userManager != null) {
                Iterator<Authorizable> allUsers = userManager.findAuthorizables("rep:principalName", null);

                while (allUsers.hasNext()) {
                    Authorizable auth = allUsers.next();
                    if (!auth.isGroup()) {
                        String id = auth.getID();
                        if (hasAdminPrivileges(auth)) {
                            adminUsers.add(id);
                        }
                    } else {
                        Group group = (Group) auth;
                        if (hasElevatedRights(group)) {
                            elevatedGroups.add(group.getID());
                        }
                    }
                }
            }

            accessHealth.put("UsersWithAdminAccess", adminUsers);
            accessHealth.put("GroupsWithElevatedRights", elevatedGroups);
        } catch (Exception e) {
            accessHealth.put("error", "Failed to evaluate access control: " + e.getMessage());
        }

        return accessHealth;
    }

    private boolean hasAdminPrivileges(Authorizable user) {
        try {
            Iterator<Group> groups = ((User) user).memberOf();
            while (groups.hasNext()) {
                String groupId = groups.next().getID().toLowerCase();
                if (groupId.contains("admin") || groupId.contains("superuser")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean hasElevatedRights(Group group) throws RepositoryException {
        String id = group.getID().toLowerCase();
        return id.contains("author") || id.contains("reviewer") || id.contains("manager");
    }

    

    private List<String> getSlowPagesViaTTFB(String rootPath, double thresholdMs, ResourceResolver resolver) {
        List<String> slowPages = new ArrayList<>();
        try {
            Resource root = resolver.getResource(rootPath);

            if (root != null) {
                for (Resource page : root.getChildren()) {
                    if ("cq:Page".equals(page.getResourceType())) {
                        String pagePath = page.getPath();
                        String url = buildPublishUrl(pagePath); // e.g., https://publish.example.com + pagePath

                        double ttfb = measureTTFB(url);
                        if (ttfb > thresholdMs) {
                            slowPages.add(pagePath + " (TTFB: " + ttfb + "ms)");
                        }
                    }
                }
            }
        } catch (Exception e) {
            slowPages.add("Error measuring TTFB: " + e.getMessage());
        }

        return slowPages.isEmpty() ? List.of("No slow pages detected") : slowPages;
    }

    private double measureTTFB(String url) {
        try {
            long start = System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.connect();
            long firstByte = System.currentTimeMillis();
            return firstByte - start;
        } catch (Exception e) {
            return -1.0;
        }
    }

    private String buildPublishUrl(String pagePath) {
        return "http://localhost:8080/" + pagePath + ".html"; // Adjust domain as needed
    }

}