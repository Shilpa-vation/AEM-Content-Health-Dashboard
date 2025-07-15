import React from "react";
export default function Grid({ rowData, tabActive }) {
    return (
        <table className="table">
            <thead>
                <tr>
                    <th>{`${tabActive === "content" ? "Page" : "Asset"} Path`}</th>
                    <th>Issue</th>
                    <th>Type</th>
                    <th>Last Modified</th>
                    <th style={{ width: "150px" }}>Status</th>
                </tr>
            </thead>
            <tbody>
                {rowData && rowData?.map((item, index) => (
                    <tr key={index}>
                        <td><a style={{ fontWeight: 700 }} target="_blank" href={item.path}>{item.path}</a></td>
                        <td>{item.issue}</td>
                        <td style={{textTransform:"capitalize"}}>{item.type}</td>
                        <td><span className={`badge badge--${item?.status?.toLowerCase() === "warn" ? "warn" : item?.status?.toLowerCase() === "error" ? "error" : "low"}`}>{item.status}</span></td>
                    </tr>
                ))}
                {rowData.length === 0 && (
                    <tr>
                        <td colSpan={5} style={{ textAlign: 'center' }}>No rows to display</td>
                    </tr>
                )}
            </tbody>
        </table>
    )
}