import React from "react";
export default function Widget({key, name, count, icon}) {
    return (
        <div key={key} class="ch-dashboard__widget">
            <div class="ch-dashboard__widget-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill=" #6D159F">{React.createElement(icon.type, icon.props)}</svg>
            </div>
            <div class="ch-dashboard__widget-content">
                <p class="type">{name}</p>
                <h2>{count}</h2>
            </div>
        </div>
    )
}
