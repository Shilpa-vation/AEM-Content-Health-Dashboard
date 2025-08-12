
import React, { useEffect, useState, useRef } from "react";
import Widget from "./widget";
import Grid from "./grid";
import Linear from "./linear";
import './dashboard.scss';
import Select from "./select";
import { Chart } from "./chart";
import { formatDateTime } from "./uttils/common";
import Loader from "./loader";

const initialWidgets = [
  { id: 1, name: 'Total Pages', key: 'totalPages', icon: <path d="M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h640q33 0 56.5 23.5T880-720v480q0 33-23.5 56.5T800-160H160Zm0-80h420v-140H160v140Zm500 0h140v-360H660v360ZM160-460h420v-140H160v140Z" /> },
  { id: 2, name: 'Total Assets', key: 'totalAssets', icon: <path d="M360-440h400L622-620l-92 120-62-80-108 140ZM120-120q-33 0-56.5-23.5T40-200v-520h80v520h680v80H120Zm160-160q-33 0-56.5-23.5T200-360v-440q0-33 23.5-56.5T280-880h200l80 80h280q33 0 56.5 23.5T920-720v360q0 33-23.5 56.5T840-280H280Zm0-80h560v-360H527l-80-80H280v440Zm0 0v-440 440Z" /> },
  { id: 3, name: 'Total Issues', key: 'issueCount', icon: <path d="m40-120 440-760 440 760H40Zm138-80h604L480-720 178-200Zm302-40q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5 28.5T480-240Zm-40-120h80v-200h-80v200Zm40-100Z" /> },
  { id: 4, name: 'Published Pages', key: 'publishedPages', icon: <path d="m424-296 282-282-56-56-226 226-114-114-56 56 170 170Zm56 216q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z" /> },
  { id: 5, name: 'Unpublished Pages', key: 'unpublishedPages', icon: <path d="M819-28 701-146q-48 32-103.5 49T480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-62 17-117.5T146-701L27-820l57-57L876-85l-57 57ZM480-160q45 0 85.5-12t76.5-33L487-360l-63 64-170-170 56-56 114 114 7-8-226-226q-21 36-33 76.5T160-480q0 133 93.5 226.5T480-160Zm335-100-59-59q21-35 32.5-75.5T800-480q0-133-93.5-226.5T480-800q-45 0-85.5 11.5T319-756l-59-59q48-31 103.5-48T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 61-17 116.5T815-260ZM602-474l-56-56 104-104 56 56-104 104Zm-64-64ZM424-424Z" /> },
  { id: 6, name: 'Replication Queue', key: 'replicationQItems', icon: <path d="M160-120q-33 0-56.5-23.5T80-200v-280h80v280h360v80H160Zm160-160q-33 0-56.5-23.5T240-360v-280h80v280h360v80H320Zm160-160q-33 0-56.5-23.5T400-520v-240q0-33 23.5-56.5T480-840h320q33 0 56.5 23.5T880-760v240q0 33-23.5 56.5T800-440H480Zm0-80h320v-160H480v160Z" /> },
  { id: 7, name: 'Active Workflow', key: 'activeWorkflowCount', icon: <path d="m136-240-56-56 296-298 160 160 208-206H640v-80h240v240h-80v-104L536-320 376-480 136-240Z" /> },
  { id: 8, name: 'Failed Workflow', key: 'failedWorkflowCount', icon: <path d="M640-240v-80h104L536-526 376-366 80-664l56-56 240 240 160-160 264 264v-104h80v240H640Z" /> },
  { id: 9, name: 'Recent Pages', key: 'recentPageCount', icon: <path d="M480-120q-75 0-140.5-28.5t-114-77q-48.5-48.5-77-114T120-480q0-75 28.5-140.5t77-114q48.5-48.5 114-77T480-840q82 0 155.5 35T760-706v-94h80v240H600v-80h110q-41-56-101-88t-129-32q-117 0-198.5 81.5T200-480q0 117 81.5 198.5T480-200q105 0 183.5-68T756-440h82q-15 137-117.5 228.5T480-120Zm112-192L440-464v-216h80v184l128 128-56 56Z" /> },
  { id: 10, name: 'Recent Assets', key: 'recentAssetCount', icon: <path d="M480-120q-75 0-140.5-28.5t-114-77q-48.5-48.5-77-114T120-480q0-75 28.5-140.5t77-114q48.5-48.5 114-77T480-840q82 0 155.5 35T760-706v-94h80v240H600v-80h110q-41-56-101-88t-129-32q-117 0-198.5 81.5T200-480q0 117 81.5 198.5T480-200q105 0 183.5-68T756-440h82q-15 137-117.5 228.5T480-120Zm112-192L440-464v-216h80v184l128 128-56 56Z" /> }
];

const days = [
  { id: 1, name: "Last 24 Hours" },
  { id: 2, name: "Last 7 Days" },
  { id: 3, name: "Last 30 Days" },
]

export default function Dashboard() {
  const [tabActive, setTabActive] = useState('sites');
  const [widgets, setWidgets] = useState(initialWidgets);
  const [rawData, setRawData] = useState({});
  const [rawDataSites, setRawDataSites] = useState([]);
  const [rawDataAssets, setRawDataAssets] = useState([]);
  const [rowData, setRowData] = useState([]);
  const [searchKey, setSearchKey] = useState("");

  const [typeFilter, setTypeFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [daysFilter, setDaysFilter] = useState("all");
  const [attr, setAttr] = useState();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState(false);
  const notifRef = useRef(null);


  useEffect(() => {
    setSearchKey("")
    if (tabActive === "sites") {
      setRowData(rawDataSites);
    }
    else {
      setRowData(rawDataAssets);
    }
  }, [tabActive]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
    } else {
      document.removeEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [open]);


  const fetchData = async (value1, value2) => {
    try {
      const host = window.location.origin;
      const query = `?value1=${encodeURIComponent(value1)}&value2=${encodeURIComponent(value2)}`;
      const apiData = await fetch(host + "/bin/content-health-audit" + query);
      const data = await apiData.json();
      setRawData(data);
      updateWidgets(data);

      let sitesDataByIssues = data?.pages?.map((item) => {
        let issues = item?.issues || [];
        return issues?.map((issue) => ({
          title: item.title,
          path: `${window.location.origin}/editor.html${item.path}.html`,
          issue: issue.message,
          type: issue.type,
          status: issue.status,
          lastModified: item.lastModified
        }));
      })

      const assetsData = data?.assets?.issues?.map(asset => ({
        title: asset?.title || '',
        path: `${window.location.origin}/assetdetails.html${asset.path}`,
        type: asset?.type || 'unknown',
        issue: Array.isArray(asset?.messages) && asset.messages.length > 0
          ? asset.messages.join(', ')
          : '',
        status: asset?.status || 'UNKNOWN'
      })) || [];
      setRawDataSites(sitesDataByIssues.flat() || []);
      setRowData(sitesDataByIssues.flat() || []);
      setRawDataAssets(assetsData);

      if (sitesDataByIssues.flat()?.length > 0) {
        const uniqueStatuses = [
          ...new Set(
            sitesDataByIssues.flat()
              .map(item => item.status?.toLowerCase())
              .filter(Boolean)
          )
        ];
        setError(uniqueStatuses.includes("error"));
        setOpen(uniqueStatuses.includes("error"))
      }


    }
    catch (error) {
      console.error("Error fetching data:", error);
    }
  }

  useEffect(() => {
    //document.addEventListener("DOMContentLoaded", () => {
    const el = document.getElementById("ch-dashboard-attr");
    if (!el) {
      console.warn("Element #ch-dashboard-attr not found");
      return;
    }
    const value1 = el.dataset.siterootpath;
    const value2 = el.dataset.damrootpath;
    setAttr({
      "value1": value1 || "",
      "value2": value2 || "",
    })
    fetchData(value1, value2);
    //});

  }, []);

  const updateWidgets = (data) => {
    let widgetData = data.widges?.[0] || {};
    setWidgets(
      initialWidgets.map(wd => {
        let count = 0;
        if (wd.key === 'totalAssets') {
          count = data.assets?.totalAssets || 0;
        } else {
          count = widgetData[wd.key] ?? 0;
        }
        return { ...wd, count };
      })
    );
  };

  const handleSearch = (value) => {
    setSearchKey(value);
    setTypeFilter("all");
    setStatusFilter("all");
    setDaysFilter("all");
    const lowerVal = value.toLowerCase();
    setRowData(
      (tabActive === "sites" ? rawDataSites : rawDataAssets).filter(item =>
        item.path.toLowerCase().includes(lowerVal) ||
        item.issue.toLowerCase().includes(lowerVal) ||
        item.type.toLowerCase().includes(lowerVal) ||
        item.status.toLowerCase().includes(lowerVal)
      )
    );
  }

  const handleFilter = (key, value) => {
    const newTypeFilter = key === "type" ? value.toLowerCase() : typeFilter;
    const newStatusFilter = key === "status" ? value.toLowerCase() : statusFilter;
    const newDaysFilter = key === "days" ? value.toLowerCase() : daysFilter;

    setTypeFilter(newTypeFilter);
    setStatusFilter(newStatusFilter);
    setDaysFilter(newDaysFilter);
    setSearchKey("");

    const baseData = tabActive === "sites" ? rawDataSites : rawDataAssets;

    const filtered = baseData.filter((item) => {
      const lastModifiedDate = new Date(item.lastModified);
      const now = new Date();
      const timeDiff = now - lastModifiedDate;

      const matchType =
        newTypeFilter === "all" || (item.type?.toLowerCase() === newTypeFilter);

      const matchStatus =
        newStatusFilter === "all" || (item.status?.toLowerCase() === newStatusFilter);

      let matchDays = true;
      if (newDaysFilter !== "all") {
        if (newDaysFilter === "last 24 hours") {
          matchDays = timeDiff <= 24 * 60 * 60 * 1000;
        } else if (newDaysFilter === "last 7 days") {
          matchDays = timeDiff <= 7 * 24 * 60 * 60 * 1000;
        } else if (newDaysFilter === "last 30 days") {
          matchDays = timeDiff <= 30 * 24 * 60 * 60 * 1000;
        }
      }

      return matchType && matchStatus && matchDays;
    });

    setRowData(filtered);
  };

  if (Object.keys(rawData).length === 0) {
    return (
      <Loader />
    )
  }

  return (
    <>
      <div class="ch-dashboard__header">
        <div class="ch-dashboard__container">
          <div className="ch-dashboard__header-content">
            <h1 className="ch-dashboard__header-title">Health Dashboard</h1>
            <div className="ch-dashboard__header-right">
              <p title="Report Generated At" className="fetch-time">
                <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#000"><path d="M200-640h560v-80H200v80Zm0 0v-80 80Zm0 560q-33 0-56.5-23.5T120-160v-560q0-33 23.5-56.5T200-800h40v-80h80v80h320v-80h80v80h40q33 0 56.5 23.5T840-720v227q-19-9-39-15t-41-9v-43H200v400h252q7 22 16.5 42T491-80H200Zm520 40q-83 0-141.5-58.5T520-240q0-83 58.5-141.5T720-440q83 0 141.5 58.5T920-240q0 83-58.5 141.5T720-40Zm67-105 28-28-75-75v-112h-40v128l87 87Z" /></svg>{formatDateTime(rawData.generatedAt)}
              </p>
              <div ref={notifRef}>
                <button
                  onClick={() => setOpen(!open)}
                  className="notification"
                  type="button"
                >
                  <svg
                    className="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-1phnduy"
                    focusable="false"
                    aria-hidden="true"
                    viewBox="0 0 24 24"
                  >
                    <path d="M19.29 17.29 18 16v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5S10.5 3.17 10.5 4v.68C7.63 5.36 6 7.92 6 11v5l-1.29 1.29c-.63.63-.19 1.71.7 1.71h13.17c.9 0 1.34-1.08.71-1.71M16 17H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5zm-4 5c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2"></path>
                  </svg>

                  {error && <span className="dot"></span>}

                  {open && error && (
                    <div className="notification_content">
                      <svg class="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-1phnduy" focusable="false" aria-hidden="true" viewBox="0 0 24 24"><path d="M12 5.99 19.53 19H4.47zM2.74 18c-.77 1.33.19 3 1.73 3h15.06c1.54 0 2.5-1.67 1.73-3L13.73 4.99c-.77-1.33-2.69-1.33-3.46 0zM11 11v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1m0 5h2v2h-2z"></path></svg>
                      <h5>Critical issues detected in the latest dashboard report.</h5>
                      <p>Please review and resolve them as soon as possible to ensure content integrity and compliance.</p>
                    </div>
                  )}
                </button>
              </div>

              <a href={`${window.location.origin}/bin/content-health-audit?${attr.value1}=/content/site&${attr.value2}=/content/dam/site&format=excel`} className="btn" type="button">Export to Excel</a>
            </div>
          </div>
        </div>
      </div >
      <div className="ch-dashboard__wraper">
        <div className="ch-dashboard__container">
          <div className="ch-dashboard__widgets">
            <div className="left">
              {widgets && widgets?.map((widget, index) => (
                <Widget key={index} name={widget.name} count={widget.count} icon={widget.icon} />
              ))}
            </div>
            {/* <div className="center">
              <div className="ch-dashboard__widget">
                <div className="ch-dashboard__widget-content">
                  <p className="type">Heap Memory usage</p>
                  <div className="chart">
                    <div className="chart__circle">
                      <Chart used={parseInt(rawData.widges?.[0].totalHeapSize)}
                        total={parseInt(rawData.widges?.[0].maxHeapSize)} />
                    </div>
                    <div className="chart__content">
                      <p>Total<span>{rawData.widges?.[0].maxHeapSize}</span></p>
                      <p>Used<span>{rawData.widges?.[0].totalHeapSize}</span></p>
                      <p>Free<span>{rawData.widges?.[0].freeHeapSize}</span></p>
                    </div>
                  </div>
                </div>
              </div>
            </div> */}
            <div className="right">
              <div className="ch-dashboard__widget">
                <div className="ch-dashboard__widget-content">
                  <p className="type">Issues By Type</p>
                  <Linear rawDataSites={rawDataSites} rawData={rawData} />
                </div>
              </div>
            </div>
          </div>
          <div className="ch-dashboard__content">
            <div className="ch-dashboard__tabs">
              <a onClick={(e) => { e.preventDefault(); setTabActive("sites") }} className={tabActive === "sites" ? "active" : ""} href="#">Sites</a>
              <a onClick={(e) => { e.preventDefault(); setTabActive("assets") }} className={tabActive === "assets" ? "active" : ""} href="#">Assets</a>
            </div>
            <div className="ch-dashboard__content-header">
              <div className="text-wraper">
                <span><svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="#000"><path d="M765-144 526-383q-30 22-65.79 34.5-35.79 12.5-76.18 12.5Q284-336 214-406t-70-170q0-100 70-170t170-70q100 0 170 70t70 170.03q0 40.39-12.5 76.18Q599-464 577-434l239 239-51 51ZM384-408q70 0 119-49t49-119q0-70-49-119t-119-49q-70 0-119 49t-49 119q0 70 49 119t119 49Z" /></svg></span>
                <input value={searchKey} onChange={(e) => handleSearch(e.target.value)} className="form-control text" type="text" placeholder="Search..." />
                {searchKey.length > 0 && <a onClick={() => {
                  setSearchKey("");
                  setRowData(rawDataSites);
                }} className="clear-search">
                  <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="#000"><path d="m338-288-50-50 141-142-141-141 50-50 142 141 141-141 50 50-141 141 141 142-50 50-141-141-142 141Z" /></svg>
                </a>}
              </div>
              <div className="ch-dashboard__content-filters">
                {tabActive === "sites" &&
                  <div className="select-warper">
                    <Select selectType={"type"} data={rawDataSites} handleFilter={handleFilter} selectedValue={typeFilter} />
                  </div>
                }
                {tabActive === "sites" &&
                  <div className="select-warper">
                    <Select selectType={"days"} data={days} handleFilter={handleFilter} selectedValue={daysFilter} />
                  </div>
                }
                <div className="select-warper">
                  <Select selectType={"status"} data={tabActive === "sites" ? rawDataSites : rawDataAssets} handleFilter={handleFilter} selectedValue={statusFilter} />
                </div>
              </div>
            </div>
            <div className="ch-dashboard__content-item">
              <Grid rowData={rowData} tabActive={tabActive} />
            </div>
          </div>
        </div>
      </div>
    </>
  );
}