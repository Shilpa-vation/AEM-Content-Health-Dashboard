package com.chd.core.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(adaptables = Resource.class)
public class DashboardModel {

    @Inject
    private ResourceResolver resolver;

    public List<Issue> getIssues() {
        List<Issue> issues = new ArrayList<>();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page root = pageManager.getPage("/content/chd");

        if (root != null) {
            Iterator<Page> allPages = root.listChildren(null, true); // deep = true

            while (allPages.hasNext()) {
                Page page = allPages.next();
                Resource contentResource = page.getContentResource();
                if (contentResource == null) continue;

                ValueMap props = contentResource.getValueMap();

                if (StringUtils.isBlank(props.get("jcr:title", ""))) {
                    issues.add(new Issue(page.getPath(), "Missing title", "WARN"));
                }
                if (StringUtils.isBlank(props.get("jcr:description", ""))) {
                    issues.add(new Issue(page.getPath(), "Missing Description", "WARN"));
                }

                // Add more checks here...
            }
        }

        return issues;
    }

    public static class Issue {
        public String pagePath, issue, status;

        public Issue(String pagePath, String issue, String status) {
            this.pagePath = pagePath;
            this.issue = issue;
            this.status = status;
        }
    }
}