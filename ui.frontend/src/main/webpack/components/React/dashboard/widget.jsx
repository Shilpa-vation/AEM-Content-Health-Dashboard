import React from "react";
export default function Widget({key, name, count, icon}) {
    return (
        <div key={key} class="ch-dashboard__widget">
            <div class="ch-dashboard__widget-icon">
                <img src={icon} alt="icon" />
            </div>
            <div class="ch-dashboard__widget-content">
                <p class="type">{name}</p>
                <h2>{count}</h2>
            </div>
        </div>
    )
}