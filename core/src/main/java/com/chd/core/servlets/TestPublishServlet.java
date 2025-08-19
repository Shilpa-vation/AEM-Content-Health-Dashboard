// package com.chd.core.servlets;
// import java.io.File;
// import java.io.IOException;
// import java.lang.management.ManagementFactory;
// import java.lang.management.MemoryUsage;
// import java.lang.management.OperatingSystemMXBean;
// import java.net.HttpURLConnection;
// import java.net.URL;
// import java.nio.file.Paths;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.stream.Collectors;

// import javax.management.ObjectName;
// import javax.servlet.Servlet;
// import javax.servlet.ServletException;

// import org.apache.http.client.methods.HttpHead;
// import org.apache.http.impl.client.CloseableHttpClient;
// import org.apache.http.impl.client.HttpClients;
// import org.apache.jackrabbit.api.security.user.Authorizable;
// import org.apache.jackrabbit.api.security.user.Group;
// import org.apache.jackrabbit.api.security.user.User;
// import org.apache.jackrabbit.api.security.user.UserManager;
// import org.apache.sling.api.SlingHttpServletRequest;
// import org.apache.sling.api.SlingHttpServletResponse;
// import org.apache.sling.api.resource.Resource;
// import org.apache.sling.api.resource.ResourceResolver;
// import org.apache.sling.api.resource.ValueMap;
// import org.apache.sling.api.servlets.SlingAllMethodsServlet;
// import org.osgi.service.component.annotations.Component;

// @Component(service = Servlet.class, property = {
//     "sling.servlet.methods=GET",
//     "sling.servlet.paths=/bin/test-publish"
// })
// public class TestPublishServlet extends SlingAllMethodsServlet {
//     @Override
//     protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
//             throws ServletException, IOException {
//         response.getWriter().write("Publish servlet is working!");
//     }

//      public Map<String, Object> getPublishHealthStats(SlingHttpServletRequest request) {
//         Map<String, Object> stats = new HashMap<>();

//         try {
//             // // ✅ Replication Agent Stats only at AEM author
//             // ObjectName agentStats = new
//             // ObjectName("com.adobe.granite:type=ReplicationAgent,agentId=publish");
//             // int successCount = (Integer) mBeanServer.getAttribute(agentStats,
//             // "SuccessCount");
//             // int failureCount = (Integer) mBeanServer.getAttribute(agentStats,
//             // "ErrorCount");
//             // long lastSuccessTime = (Long) mBeanServer.getAttribute(agentStats,
//             // "LastSuccessTime");
//             // long lastFailureTime = (Long) mBeanServer.getAttribute(agentStats,
//             // "LastErrorTime");

//             // stats.put("SuccessCount", successCount);
//             // stats.put("FailureCount", failureCount);
//             // stats.put("LastSuccessTime", lastSuccessTime);
//             // stats.put("LastFailureTime", lastFailureTime);

//             // int total = successCount + failureCount;
//             // stats.put("SuccessRate", total > 0 ? (successCount * 100.0) / total : 0.0);
//             // stats.put("FailureRate", total > 0 ? (failureCount * 100.0) / total : 0.0);

//             // // 📊 Replication Queue
//             // ObjectName queueStats = new
//             // ObjectName("com.adobe.granite:type=ReplicationQueue,queueId=publish");
//             // int queueLength = (Integer) mBeanServer.getAttribute(queueStats,
//             // "QueueLength");
//             // stats.put("QueueLength", queueLength);
//             ResourceResolver resolver = request.getResourceResolver();

//             // 🔍 System Errors
//             stats.put("MissingTemplates", scanForMissingTemplates("/content", resolver));
//             stats.put("BrokenLinks", scanForBrokenLinks("/content", resolver));
//             // stats.put("MissingResources", scanForMissingResources("/content", resolver));

//             // ⏱️ Response Time Errors
//             stats.put("SlowPages", getSlowPagesViaTTFB("/content", 500, resolver));

//             // 📁 Critical Logs
//             // as we have developer console stats.put("RecentCriticalErrors",
//             // readCriticalErrorsFromSlingLog());

//             // 🧠 CPU & Memory
//             // cannot get as its from AEM cloud stats.put("CpuUtilization", getCpuLoad());
//             MemoryUsage heap = memoryBean.getHeapMemoryUsage();
//             long usedMemMB = heap.getUsed() / (1024 * 1024);
//             long maxMemMB = heap.getMax() / (1024 * 1024);
//             stats.put("MemoryUsedMB", usedMemMB);
//             stats.put("MemoryMaxMB", maxMemMB);
//             stats.put("MemoryUsagePercent", maxMemMB > 0 ? (usedMemMB * 100.0) / maxMemMB : 0.0);

//             // 💾 Disk Space
//             File root = new File("/");
//             long freeDiskGB = root.getFreeSpace() / (1024 * 1024 * 1024);
//             long totalDiskGB = root.getTotalSpace() / (1024 * 1024 * 1024);
//             stats.put("DiskFreeGB", freeDiskGB);
//             stats.put("DiskTotalGB", totalDiskGB);
//             stats.put("DiskUsagePercent", totalDiskGB > 0 ? ((totalDiskGB - freeDiskGB) * 100.0) / totalDiskGB : 0.0);

//             // 👥 Active Sessions
//             ObjectName sessionStats = new ObjectName("org.apache.sling:type=SessionStatistics");
//             int activeSessions = (Integer) mBeanServer.getAttribute(sessionStats, "ActiveSessions");
//             stats.put("ActiveSessions", activeSessions);

//             // // 🚀 Content Performance Metrics
//             // stats.put("AveragePageLoadTimeMs",
//             // parsePageLoadTimes("/opt/aem/logs/access.log"));
//             // stats.put("ContentDeliveryLatencyMs",
//             // estimateDeliveryLatency("/opt/aem/logs/access.log"));
//             // stats.put("CacheHitRatio", parseCacheStats("/opt/aem/logs/dispatcher.log"));
//             // stats.put("AssetDeliveryPerformance",
//             // parseAssetDelivery("/opt/aem/logs/access.log"));

//             // 🚀 Synthetic Content Performance Metrics
//             List<Map<String, Object>> pageMetrics = getSyntheticPageMetrics("/content", 500, request);
//             stats.put("SyntheticPageMetrics", pageMetrics);

//             // 🔄 Content Version Control / Publish History -Both are more relevant to
//             // author
//             // stats.put("PublishedVersions", getPublishedVersions("/content", request));
//             // stats.put("RecentPublishActivity", getRecentPublishActivity());

//             // 📈 Traffic and User Interaction Metrics - Analytics helps in this
//             // stats.put("ContentEngagement", getContentEngagementMetrics());
//             // stats.put("RealTimeDelivery", getRealTimeDeliveryMetrics());
//             // stats.put("PersonalizationMetrics", getPersonalizationMetrics());

//             // 🧠 AEM Publisher Specific Health Checks
//             // stats.put("DispatcherHealth", getDispatcherHealth());
//             // stats.put("ReplicationAgentHealth", getReplicationAgentHealth());
//             // stats.put("ClusterStatus", getClusterStatus());

//             // 🔐 Security and Compliance Checks
//             stats.put("AccessControlHealth", getAccessControlHealth(resolver));
//             //stats.put("AuditLogs", getRecentAuditLogs("/opt/aem/logs/audit.log"));
//             stats.put("GDPRViolations", getGDPRViolations("/content"));

//         } catch (Exception e) {
//             stats.put("error", "Monitoring failed: " + e.getMessage());
//         }

//         return stats;
//     }

//     private List<Map<String, Object>> getSyntheticPageMetrics(String rootPath, double thresholdMs,
//             SlingHttpServletRequest request) {
//         List<Map<String, Object>> pageMetrics = new ArrayList<>();

//         ResourceResolver resolver = request.getResourceResolver();
//         try {
//             Resource root = resolver.getResource(rootPath);

//             if (root != null) {
//                 for (Resource page : root.getChildren()) {
//                     if ("cq:Page".equals(page.getResourceType())) {
//                         String pagePath = page.getPath();
//                         String url = buildPublishUrl(pagePath);

//                         double ttfb = measureTTFB(url);
//                         boolean isSlow = ttfb > thresholdMs;

//                         List<Map<String, Object>> assetMetrics = getAssetDeliveryMetrics(page);

//                         pageMetrics.add(Map.of(
//                                 "path", pagePath,
//                                 "url", url,
//                                 "TTFBms", ttfb,
//                                 "isSlow", isSlow,
//                                 "AssetDelivery", assetMetrics));
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             pageMetrics.add(Map.of("error", "Synthetic monitoring failed: " + e.getMessage()));
//         }

//         return pageMetrics;
//     }

//     private List<Map<String, Object>> getAssetDeliveryMetrics(Resource page) {
//         List<Map<String, Object>> assets = new ArrayList<>();
//         Resource content = page.getChild("jcr:content");

//         if (content != null) {
//             for (Resource child : content.getChildren()) {
//                 ValueMap props = child.getValueMap();
//                 if (props.containsKey("fileReference")) {
//                     String assetPath = props.get("fileReference", String.class);
//                     String assetUrl = buildPublishUrl(assetPath);
//                     double ttfb = measureTTFB(assetUrl);

//                     assets.add(Map.of(
//                             "assetPath", assetPath,
//                             "TTFBms", ttfb));
//                 }
//             }
//         }

//         return assets;
//     }

//     // 🔧 Utility Methods
//     // Since it AEM cloud we cannot get this
//     private double getCpuLoad() {
//         try {
//             OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
//             double loadAvg = osBean.getSystemLoadAverage();

//             if (loadAvg >= 0) {
//                 return loadAvg; // Not a percentage, but indicative of system load
//             } else {
//                 return -1.0; // Not supported
//             }

//         } catch (Exception e) {
//             return -1.0;
//         }
//     }

//     private List<String> scanForMissingTemplates(String rootPath, ResourceResolver resolver) {
//         List<String> missingTemplates = new ArrayList<>();

//         try {
//             Resource root = resolver.getResource(rootPath);

//             if (root != null) {
//                 scanPagesRecursively(root, missingTemplates);
//             }
//         } catch (Exception e) {
//             missingTemplates.add("Error scanning for missing templates: " + e.getMessage());
//         }

//         return missingTemplates;
//     }

//     private void scanPagesRecursively(Resource resource, List<String> missingTemplates) {
//         if ("cq:Page".equals(resource.getResourceType())) {
//             Resource content = resource.getChild("jcr:content");
//             if (content != null) {
//                 ValueMap props = content.getValueMap();
//                 if (!props.containsKey("cq:template")) {
//                     missingTemplates.add(resource.getPath() + " → missing cq:template");
//                 }
//             } else {
//                 missingTemplates.add(resource.getPath() + " → missing jcr:content");
//             }
//         }

//         for (Resource child : resource.getChildren()) {
//             scanPagesRecursively(child, missingTemplates);
//         }
//     }

//     private List<String> scanForBrokenLinks(String rootPath, ResourceResolver resolver) {
//         List<String> brokenLinks = new ArrayList<>();

//         try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//             Resource root = resolver.getResource(rootPath);

//             if (root != null) {
//                 for (Resource page : root.getChildren()) {
//                     Resource content = page.getChild("jcr:content");
//                     if (content != null) {
//                         for (Resource child : content.getChildren()) {
//                             ValueMap props = child.getValueMap();
//                             for (Map.Entry<String, Object> entry : props.entrySet()) {
//                                 String key = entry.getKey();
//                                 Object value = entry.getValue();

//                                 if (value instanceof String) {
//                                     String url = (String) value;
//                                     if ((key.contains("href") || key.contains("src") || key.contains("link")
//                                             || key.contains("fileReference"))) {
//                                         if (url.startsWith("http")) {
//                                             try {
//                                                 HttpHead request = new HttpHead(url);
//                                                 int status = httpClient.execute(request).getStatusLine()
//                                                         .getStatusCode();
//                                                 if (status >= 400) {
//                                                     brokenLinks.add(page.getPath() + " → Broken external link: " + url);
//                                                 }
//                                             } catch (Exception e) {
//                                                 brokenLinks.add(page.getPath() + " → Broken link (exception): " + url);
//                                             }
//                                         } else if (url.startsWith("/content") && resolver.getResource(url) == null) {
//                                             brokenLinks.add(page.getPath() + " → Broken internal reference: " + url);
//                                         }
//                                     }
//                                 }
//                             }
//                         }
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             brokenLinks.add("Error scanning for broken links: " + e.getMessage());
//         }

//         return brokenLinks;
//     }

//     private List<String> scanForMissingResources(String rootPath, ResourceResolver resolver) {
//         List<String> missingAssets = new ArrayList<>();

//         try {
//             Resource root = resolver.getResource(rootPath);

//             if (root != null) {
//                 for (Resource page : root.getChildren()) {
//                     Resource content = page.getChild("jcr:content");
//                     if (content != null) {
//                         for (Resource child : content.getChildren()) {
//                             ValueMap props = child.getValueMap();
//                             String assetPath = props.get("fileReference", String.class);

//                             if (assetPath != null) {
//                                 Resource asset = resolver.getResource(assetPath);
//                                 if (asset == null) {
//                                     missingAssets.add(page.getPath() + " → missing asset: " + assetPath);
//                                 }
//                             }
//                         }
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             missingAssets.add("Error scanning for missing resources: " + e.getMessage());
//         }

//         return missingAssets;
//     }

//     private List<String> readCriticalErrors(String logPath) {
//         try (Stream<String> lines = Files.lines(Paths.get(logPath))) {
//             return lines.filter(line -> line.contains("CRITICAL") || line.contains("ReplicationException"))
//                     .limit(20)
//                     .collect(Collectors.toList());
//         } catch (IOException e) {
//             return Arrays.asList("Error reading log: " + e.getMessage());
//         }
//     }

//     private double parsePageLoadTimes(String logPath) {
//         try (Stream<String> lines = Files.lines(Paths.get(logPath))) {
//             return lines.filter(line -> line.contains("GET /content"))
//                     .mapToDouble(this::extractResponseTime)
//                     .average().orElse(0.0);
//         } catch (IOException e) {
//             return -1.0;
//         }
//     }

//     private double estimateDeliveryLatency(String logPath) {
//         return parsePageLoadTimes(logPath); // Simplified proxy
//     }

//     private double parseCacheStats(String logPath) {
//         try (Stream<String> lines = Files.lines(Paths.get(logPath))) {
//             long hits = lines.filter(line -> line.contains("cache-hit")).count();
//             long misses = lines.filter(line -> line.contains("cache-miss")).count();
//             long total = hits + misses;
//             return total > 0 ? (hits * 100.0) / total : 0.0;
//         } catch (IOException e) {
//             return -1.0;
//         }
//     }

//     private Map<String, Double> parseAssetDelivery(String logPath) {
//         try (Stream<String> lines = Files.lines(Paths.get(logPath))) {
//             return lines.filter(line -> line.contains("/content/dam"))
//                     .collect(Collectors.groupingBy(
//                             this::extractAssetPath,
//                             Collectors.averagingDouble(this::extractResponseTime)));
//         } catch (IOException e) {
//             return Collections.singletonMap("error", -1.0);
//         }
//     }

//     private double extractResponseTime(String logLine) {
//         try {
//             String[] parts = logLine.split(" ");
//             return Double.parseDouble(parts[parts.length - 1]);
//         } catch (Exception e) {
//             return 0.0;
//         }
//     }

//     private String extractAssetPath(String logLine) {
//         try {
//             int start = logLine.indexOf("GET ");
//             int end = logLine.indexOf(" HTTP");
//             return logLine.substring(start + 4, end);
//         } catch (Exception e) {
//             return "unknown";
//         }
//     }

//     // private List<Map<String, Object>> getPublishedVersions(String rootPath,
//     // SlingHttpServletRequest request) {
//     // List<Map<String, Object>> versions = new ArrayList<>();

//     // try {
//     // ResourceResolver resolver = request.getResourceResolver();
//     // Resource root = resolver.getResource(rootPath);

//     // if (root != null) {
//     // for (Resource page : root.getChildren()) {
//     // String path = page.getPath();

//     // // Only include cq:Page nodes
//     // if ("cq:Page".equals(page.getResourceType())) {
//     // String version = getVersionForPage(page);
//     // String status = getPublishStatus(page);
//     // String timestamp = getLastPublishedTimestamp(page);

//     // versions.add(Map.of(
//     // "path", path,
//     // "version", version,
//     // "status", status,
//     // "timestamp", timestamp));
//     // }
//     // }
//     // }
//     // } catch (Exception e) {
//     // versions.add(Map.of("error", "Failed to retrieve published versions: " +
//     // e.getMessage()));
//     // }

//     // return versions;
//     // }

//     // private String getVersionForPage(Resource page) {
//     // return "v" + (1 + new Random().nextInt(5)); // Simulated version like v1–v5
//     // }

//     // private String getPublishStatus(Resource page) {
//     // return new Random().nextBoolean() ? "success" : "failed"; // Simulated status
//     // }

//     // private String getLastPublishedTimestamp(Resource page) {
//     // int hour = 9 + new Random().nextInt(4);
//     // return "2025-08-12T" + hour + ":00:00"; // Simulated timestamp
//     // }

//     // private List<Map<String, Object>> getRecentPublishActivity() {
//     // return List.of(
//     // Map.of("path", "/content/site/page3", "type", "page", "status", "success",
//     // "timestamp",
//     // "2025-08-12T11:00:00"),
//     // Map.of("path", "/content/dam/image1.jpg", "type", "asset", "status",
//     // "success", "timestamp",
//     // "2025-08-12T10:50:00"));
//     // }

//     // 📈 Traffic and Interaction
//     // private Map<String, Object> getContentEngagementMetrics() {
//     // return Map.of(
//     // "PageViews", 12450,
//     // "Clicks", 3420,
//     // "Interactions", 980);
//     // }

//     // private Map<String, Object> getRealTimeDeliveryMetrics() {
//     // return Map.of(
//     // "AverageTTFBms", 180.5,
//     // "PeakTTFBms", 420.0);
//     // }

//     // private Map<String, Object> getPersonalizationMetrics() {
//     // return Map.of(
//     // "PersonalizedViews", 3200,
//     // "FallbackViews", 800,
//     // "PersonalizationSuccessRate", 80.0);
//     // }

//     // 🧠 Publisher Health Checks
//     // private Map<String, Object> getDispatcherHealth() {
//     // return Map.of(
//     // "Status", "Healthy",
//     // "CacheHitRatio", 87.3,
//     // "LastCacheFlush", "2025-08-12T08:00:00");
//     // }

//     // private Map<String, Object> getReplicationAgentHealth() {
//     // return Map.of(
//     // "AgentId", "publish",
//     // "Status", "Active",
//     // "LastSuccess", "2025-08-12T11:05:00",
//     // "LastFailure", "2025-08-11T17:30:00");
//     // }

//     // private List<Map<String, Object>> getClusterStatus() {
//     // return List.of(
//     // Map.of("Instance", "publisher1", "Status", "Synced"),
//     // Map.of("Instance", "publisher2", "Status", "Lagging"));
//     // }

//     // 🔐 Security and Compliance
//     private Map<String, Object> getAccessControlHealth(ResourceResolver resolver) {
//         Map<String, Object> accessHealth = new HashMap<>();
//         List<String> adminUsers = new ArrayList<>();
//         List<String> elevatedGroups = new ArrayList<>();

//         try {
//             UserManager userManager = resolver.adaptTo(UserManager.class);
//             if (userManager != null) {
//                 Iterator<Authorizable> allUsers = userManager.findAuthorizables("rep:principalName", null);

//                 while (allUsers.hasNext()) {
//                     Authorizable auth = allUsers.next();
//                     if (!auth.isGroup()) {
//                         String id = auth.getID();
//                         if (hasAdminPrivileges(auth)) {
//                             adminUsers.add(id);
//                         }
//                     } else {
//                         Group group = (Group) auth;
//                         if (hasElevatedRights(group)) {
//                             elevatedGroups.add(group.getID());
//                         }
//                     }
//                 }
//             }

//             accessHealth.put("UsersWithAdminAccess", adminUsers);
//             accessHealth.put("GroupsWithElevatedRights", elevatedGroups);
//         } catch (Exception e) {
//             accessHealth.put("error", "Failed to evaluate access control: " + e.getMessage());
//         }

//         return accessHealth;
//     }

//     private boolean hasAdminPrivileges(Authorizable user) {
//         try {
//             Iterator<Group> groups = ((User) user).memberOf();
//             while (groups.hasNext()) {
//                 String groupId = groups.next().getID().toLowerCase();
//                 if (groupId.contains("admin") || groupId.contains("superuser")) {
//                     return true;
//                 }
//             }
//         } catch (Exception ignored) {
//         }
//         return false;
//     }

//     private boolean hasElevatedRights(Group group) {
//         String id = group.getID().toLowerCase();
//         return id.contains("author") || id.contains("reviewer") || id.contains("manager");
//     }

//     private List<String> getRecentAuditLogs(String logPath) {
//         try (Stream<String> lines = Files.lines(Paths.get(logPath))) {
//             return lines.filter(line -> line.contains("LOGIN") || line.contains("ACCESS"))
//                     .limit(20)
//                     .collect(Collectors.toList());
//         } catch (IOException e) {
//             return List.of("Error reading audit logs: " + e.getMessage());
//         }
//     }

//     // private List<String> getGDPRViolations(String rootPath) {
//     //     return List.of(
//     //             "/content/site/userdata/profile1",
//     //             "/content/site/forms/contact-form");
//     // }

//     private List<String> getSlowPagesViaTTFB(String rootPath, double thresholdMs, ResourceResolver resolver) {
//         List<String> slowPages = new ArrayList<>();
//         try {
//             Resource root = resolver.getResource(rootPath);

//             if (root != null) {
//                 for (Resource page : root.getChildren()) {
//                     if ("cq:Page".equals(page.getResourceType())) {
//                         String pagePath = page.getPath();
//                         String url = buildPublishUrl(pagePath); // e.g., https://publish.example.com + pagePath

//                         double ttfb = measureTTFB(url);
//                         if (ttfb > thresholdMs) {
//                             slowPages.add(pagePath + " (TTFB: " + ttfb + "ms)");
//                         }
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             slowPages.add("Error measuring TTFB: " + e.getMessage());
//         }

//         return slowPages.isEmpty() ? List.of("No slow pages detected") : slowPages;
//     }

//     private double measureTTFB(String url) {
//         try {
//             long start = System.currentTimeMillis();
//             HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
//             conn.setRequestMethod("GET");
//             conn.setConnectTimeout(3000);
//             conn.setReadTimeout(3000);
//             conn.connect();
//             long firstByte = System.currentTimeMillis();
//             return firstByte - start;
//         } catch (Exception e) {
//             return -1.0;
//         }
//     }

//     private String buildPublishUrl(String pagePath) {
//         return "http://localhost:8080/" + pagePath + ".html"; // Adjust domain as needed
//     }

//     private String extractPagePath(String logLine) {
//         try {
//             int start = logLine.indexOf("GET ");
//             int end = logLine.indexOf(" HTTP");
//             return logLine.substring(start + 4, end);
//         } catch (Exception e) {
//             return "unknown";
//         }
//     }
// }