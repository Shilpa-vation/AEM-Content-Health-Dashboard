import React from "react";

export default function Linear({ data }) {
    const typeCounts = data.reduce((acc, item) => {
        acc[item.type] = (acc[item.type] || 0) + 1;
        return acc;
    }, {});

    const linears = Object.entries(typeCounts).map(([key, value]) => ({
        type: key,
        count: value,
        width: (value / data.length) * 100,
        bg: key === "metadata" ? "#FFF8F3" : key === "seo" ? "#EEEDFF" : "#F8E7FE",
        color: key === "metadata" ? "#FFB780" : key === "seo" ? "#766FFF" : "#B90EF2",
    }));

    return (
        <div className="ch-dashboard__linears">
            {linears?.map((linear, index) => (
                <div className="ch-dashboard__linear" key={index}>
                    <div className="linear-left"><span style={{ textTransform: "capitalize" }}>{linear.type}</span><span>{linear.count}</span></div>
                    <div className="linear-right" style={{ backgroundColor: linear.bg }}>
                        <div className="linear-right-tablet" style={{ backgroundColor: linear.color, width: `${linear.width}%` }}></div>
                    </div>
                </div>
            ))}
        </div>
    );
}