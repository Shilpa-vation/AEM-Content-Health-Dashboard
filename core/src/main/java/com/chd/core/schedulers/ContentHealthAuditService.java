// package com.chd.core.s;

// import com.day.cq.dam.api.Asset;
// import com.day.cq.wcm.api.Page;
// import com.day.cq.wcm.api.PageManager;
// import org.apache.commons.lang3.StringUtils;
// import org.apache.http.client.methods.HttpHead;
// import org.apache.http.impl.client.CloseableHttpClient;
// import org.apache.http.impl.client.HttpClients;
// import org.apache.sling.api.resource.*;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Reference;

// import javax.jcr.Node;
// import javax.jcr.Session;
// import java.time.Instant;
// import java.time.temporal.ChronoUnit;
// import java.util.*;

// @Component(service = Runnable.class, immediate = true)
// public class ContentHealthAuditService implements Runnable {

//     @Reference
//     private ResourceResolverFactory resolverFactory;

//     private static final String ROOT_PATH = "/content/chd";
//     private static final long STALE_DAYS = 180;
//     private static final long MAX_IMAGE_SIZE = 2_000_000; // 2MB

//     @Override
//     public void run() {
//         try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(null);
//              CloseableHttpClient httpClient = HttpClients.createDefault()) {

//             PageManager pageManager = resolver.adaptTo(PageManager.class);
//             Page root = pageManager.getPage(ROOT_PATH);

//             if (root != null) {
//                 Iterator<Page> pages = root.listChildren(null, true);
//                 while (pages.hasNext()) {
//                     Page page = pages.next();
//                     Resource content = page.getContentResource();
//                     if (content == null) continue;

//                     ValueMap props = content.getValueMap();
//                     String path = page.getPath();

//                     // 1. Metadata Completeness
//                     checkMetadata(path, props);

//                     // 2. SEO Tags
//                     checkSEO(path, props);

//                     // 3. Content Audit
//                     checkAudit(path, props);

//                     // 4. Broken Links
//                     checkLinks(path, content, httpClient);

//                     // 5. Workflow
//                     checkWorkflow(path, props);

//                     // 6. Asset Optimization
//                     checkAssets(resolver, path);
//                 }
//             }

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     private void checkMetadata(String path, ValueMap props) {
//         if (StringUtils.isBlank(props.get("jcr:title", ""))) log(path, "Missing title");
//         if (StringUtils.isBlank(props.get("jcr:description", ""))) log(path, "Missing description");
//         if (props.get("cq:tags", new String[]{}).length == 0) log(path, "Missing tags");
//         if (StringUtils.isBlank(props.get("language", ""))) log(path, "Missing language");
//     }

//     private void checkSEO(String path, ValueMap props) {
//         if (StringUtils.isBlank(props.get("canonicalUrl", ""))) log(path, "Missing canonical URL");
//         if (StringUtils.isBlank(props.get("robots", ""))) log(path, "Missing robots meta tag");
//         if (StringUtils.isBlank(props.get("og:title", ""))) log(path, "Missing Open Graph title");
//         if (StringUtils.isBlank(props.get("twitter:title", ""))) log(path, "Missing Twitter Card title");
//     }

//     private void checkAudit(String path, ValueMap props) {
//         Calendar lastModified = props.get("cq:lastModified", Calendar.class);
//         if (lastModified != null) {
//             Instant lastMod = lastModified.toInstant();
//             if (lastMod.isBefore(Instant.now().minus(STALE_DAYS, ChronoUnit.DAYS))) {
//                 log(path, "Stale content (not updated in 6+ months)");
//             }
//         }

//         String replication = props.get("cq:lastReplicationAction", "");
//         if (!"Activate".equals(replication)) {
//             log(path, "Page not published");
//         }
//     }

//     private void checkLinks(String path, Resource content, CloseableHttpClient httpClient) {
//         content.getChildren().forEach(comp -> {
//             comp.getValueMap().forEach((key, value) -> {
//                 if (value instanceof String && (key.contains("href") || key.contains("src"))) {
//                     String url = (String) value;
//                     if (url.startsWith("http")) {
//                         try {
//                             HttpHead request = new HttpHead(url);
//                             int status = httpClient.execute(request).getStatusLine().getStatusCode();
//                             if (status >= 400) log(path, "Broken external link: " + url);
//                         } catch (Exception e) {
//                             log(path, "Broken link (exception): " + url);
//                         }
//                     } else if (url.startsWith("/content") && content.getResourceResolver().getResource(url) == null) {
//                         log(path, "Broken internal reference: " + url);
//                     }
//                 }
//             });
//         });
//     }

//     private void checkWorkflow(String path, ValueMap props) {
//         String status = props.get("cq:workflowStatus", "");
//         if ("IN_PROGRESS".equals(status)) log(path, "Page stuck in workflow");
//     }

//     private void checkAssets(ResourceResolver resolver, String pagePath) {
//         Resource damRoot = resolver.getResource("/content/dam/your-site");
//         if (damRoot == null) return;

//         Set<String> seenAssets = new HashSet<>();
//         for (Resource assetRes : damRoot.getChildren()) {
//             Asset asset = assetRes.adaptTo(Asset.class);
//             if (asset == null) continue;

//             String mime = asset.getMimeType();
//             long size = asset.getOriginal().getSize();
//             String alt = asset.getMetadataValue("dc:description");

//             if (size > MAX_IMAGE_SIZE) log(pagePath, "Large asset: " + asset.getPath() + " (" + size + " bytes)");
//             if (StringUtils.isBlank(alt)) log(pagePath, "Missing alt text: " + asset.getPath());

//             String assetName = asset.getName();
//             if (!seenAssets.add(assetName)) {
//                 log(pagePath, "Duplicate asset detected: " + assetName);
//             }
//         }
//     }

//     private void log(String pagePath, String issue) {
//         System.out.println("[WARN] " + pagePath + " → " + issue);
//     }
// }