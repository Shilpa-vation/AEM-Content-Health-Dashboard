import React from "react";
import useLinear from './hooks/useLinear'

export default function Linear({ rawDataSites, rawData }) {
    let sitesData = useLinear(rawDataSites);
    let assetData = {
        name: "Assets",
        count: rawData?.assets?.issueCount || 0,
        width: (rawData?.assets?.totalAssets / rawData?.assets?.issueCount) * 100,
        bg: "#EEF7EE",
        color: "#4CAF50"
    }
    let linears = [...sitesData, assetData];

    return (
        <div className="ch-dashboard__linears">
            {linears?.map((linear, index) => (
                <div className="ch-dashboard__linear" key={index}>
                    <div className="linear-left"><span style={{ textTransform: "capitalize" }}>{linear.name}</span><span>{linear.count}</span></div>
                    <div className="linear-right" style={{ backgroundColor: linear.bg }}>
                        <div className="linear-right-tablet" style={{ backgroundColor: linear.color, width: `${linear.width}%` }}></div>
                    </div>
                </div>
            ))}
        </div>
    );
}