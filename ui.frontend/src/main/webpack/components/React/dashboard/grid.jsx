import React from "react";
import { formatDate } from './uttils/common';
export default function Grid({ rowData, tabActive }) {
    return (
        <table className="table">
            <thead>
                <tr>
                    <th>{`${tabActive === "sites" ? "Page" : "Asset"} Name`}</th>
                    <th>Issue</th>
                    <th>Type</th>
                    {tabActive === "sites" && <th>Last Modified</th>}
                    <th style={{ width: "85px" }}>Status</th>
                    <th style={{ width: "0px" }}></th>
                </tr>
            </thead>
            <tbody className="scrollable-tbody">
                {rowData && rowData?.map((item, index) => (
                    <tr key={index}>
                        <td><a className="grid-title-link" target="_blank" href={item.path}>{item.title}</a></td>
                        <td>{item.issue}</td>
                        <td style={{ textTransform: "capitalize" }}>{item.type}</td>
                        {tabActive === "sites" && <td>{formatDate(item.lastModified)}</td>}

                        <td><span className={`badge badge--${item?.status?.toLowerCase()}`}>{item.status}</span></td>
                        <td><a target="_blank" href={item.path} className="grid-action"><svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="#6D159F"><path d="m243-240-51-51 405-405H240v-72h480v480h-72v-357L243-240Z" /></svg></a></td>
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