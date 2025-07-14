import React from "react";
export default function Navigation() {
    return (
        <div class="ch-dashboard__header">
            <div class="ch-dashboard__container">
                <h1>Health Dashboard</h1>
            </div>
                   <div className="ch-dashboard__content-tabs">
              <a onClick={(e) => { e.preventDefault(); setTabActive("content") }} className={tabActive === "content" ? "active" : ""} href="#">Content</a>
              <a onClick={(e) => { e.preventDefault(); setTabActive("assets") }} className={tabActive === "assets" ? "active" : ""} href="#">Assets</a>
            </div>
        </div>
    )
}