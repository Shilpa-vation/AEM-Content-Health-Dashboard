
import React, { useEffect } from "react";
import Widget from "./widget";
import Navigation from "./navigation";
import Grid from "./grid";
import Linear from "./linear";
import './dashboard.scss';
import useLinear from './hooks/useLinear';
import Select from "./select";
// import pageIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/page.svg';
// import warningIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/warning.svg';
// import publishedIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/published.svg';
// import unpublishedIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/unpublished.svg';


const initialWidgets = [
  { id: 1, name: 'Total Sites', key: 'totalPages', icon: <path d="M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h640q33 0 56.5 23.5T880-720v480q0 33-23.5 56.5T800-160H160Zm0-80h420v-140H160v140Zm500 0h140v-360H660v360ZM160-460h420v-140H160v140Z" /> },
  { id: 2, name: 'Published Sites', key: 'publishedPages', icon: <path d="m424-296 282-282-56-56-226 226-114-114-56 56 170 170Zm56 216q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z" /> },
  { id: 3, name: 'Unpublished Sites', key: 'unpublishedPages', icon: <path d="M819-28 701-146q-48 32-103.5 49T480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-62 17-117.5T146-701L27-820l57-57L876-85l-57 57ZM480-160q45 0 85.5-12t76.5-33L487-360l-63 64-170-170 56-56 114 114 7-8-226-226q-21 36-33 76.5T160-480q0 133 93.5 226.5T480-160Zm335-100-59-59q21-35 32.5-75.5T800-480q0-133-93.5-226.5T480-800q-45 0-85.5 11.5T319-756l-59-59q48-31 103.5-48T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 61-17 116.5T815-260ZM602-474l-56-56 104-104 56 56-104 104Zm-64-64ZM424-424Z" /> },
  { id: 4, name: 'Total Issues', key: 'totalIssues', icon: <path d="m40-120 440-760 440 760H40Zm138-80h604L480-720 178-200Zm302-40q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5 28.5T480-240Zm-40-120h80v-200h-80v200Zm40-100Z" /> },
  { id: 5, name: 'Active Workflow', key: 'totalPages', icon: <path d="m136-240-56-56 296-298 160 160 208-206H640v-80h240v240h-80v-104L536-320 376-480 136-240Z" /> },
  { id: 6, name: 'Failed Workflow', key: 'publishedPages', icon: <path d="M640-240v-80h104L536-526 376-366 80-664l56-56 240 240 160-160 264 264v-104h80v240H640Z" /> },
  { id: 7, name: 'Total Assets', key: 'totalIssues', icon: <path d="M360-440h400L622-620l-92 120-62-80-108 140ZM120-120q-33 0-56.5-23.5T40-200v-520h80v520h680v80H120Zm160-160q-33 0-56.5-23.5T200-360v-440q0-33 23.5-56.5T280-880h200l80 80h280q33 0 56.5 23.5T920-720v360q0 33-23.5 56.5T840-280H280Zm0-80h560v-360H527l-80-80H280v440Zm0 0v-440 440Z" /> },
  { id: 8, name: 'Replication Items In Queue', key: 'unpublishedPages', icon: <path d="M160-120q-33 0-56.5-23.5T80-200v-280h80v280h360v80H160Zm160-160q-33 0-56.5-23.5T240-360v-280h80v280h360v80H320Zm160-160q-33 0-56.5-23.5T400-520v-240q0-33 23.5-56.5T480-840h320q33 0 56.5 23.5T880-760v240q0 33-23.5 56.5T800-440H480Zm0-80h320v-160H480v160Z" /> },
  { id: 9, name: 'Unpublished Sites', key: 'unpublishedPages', icon: <path d="M819-28 701-146q-48 32-103.5 49T480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-62 17-117.5T146-701L27-820l57-57L876-85l-57 57ZM480-160q45 0 85.5-12t76.5-33L487-360l-63 64-170-170 56-56 114 114 7-8-226-226q-21 36-33 76.5T160-480q0 133 93.5 226.5T480-160Zm335-100-59-59q21-35 32.5-75.5T800-480q0-133-93.5-226.5T480-800q-45 0-85.5 11.5T319-756l-59-59q48-31 103.5-48T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 61-17 116.5T815-260ZM602-474l-56-56 104-104 56 56-104 104Zm-64-64ZM424-424Z" /> }
];

const status = [
  { name: 'Error', color: 0 },
  { name: 'Warn', color: 0 },
  { name: 'Low', color: 0 },
]

const types = [
  { name: 'Metadata', color: 0 },
  { name: 'SEO', color: 0 },
  { name: 'Audit', color: 0 },
  { name: 'Workflow', color: 0 },
]

export default function Dashboard() {
  const [tabActive, setTabActive] = React.useState('content');
  const [widgets, setWidgets] = React.useState(initialWidgets);
  const [rawData, setRawData] = React.useState([]);
  const [rawDataAssets, setRawDataAssets] = React.useState([]);
  const [rowData, setRowData] = React.useState([]);
  const [searchKey, setSearchKey] = React.useState("");
  const [types, setTypes] = React.useState([]);

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    setRowData()
    if (tabActive === "content") {
      setRowData(rawData);
    }
    else {
      console.log(rawDataAssets)
      setRowData(rawDataAssets);
    }
  }, [tabActive]);

  // useEffect(() => {
  //   if (rawData.length > 0) {
  //     const linearItems = useLinear(rawData);
  //     setTypes(linearItems)
  //   }
  // }, [rawData])

  const fetchData = async () => {
    try {
      const rawdata = await fetch("http://localhost:4502/bin/content-health-audit");
      const data = await rawdata.json();
      updateWidgets(data);

      let contentDataByIssues = data?.pages?.map((item) => {
        let issues = item?.issues || [];
        return issues?.map((issue) => ({
          path: item.path,
          issue: issue.message,
          type: issue.type,
          status: issue.status,
        }));
      })

      const assetsData = data?.assets?.issues?.map(asset => ({
        path: asset?.path || '',
        type: asset?.type || 'unknown',
        issue: Array.isArray(asset?.messages) && asset.messages.length > 0
          ? asset.messages.join(', ')
          : '',
        status: asset?.status || 'UNKNOWN'
      })) || [];

      setRawData(contentDataByIssues.flat() || []);
      setRowData(contentDataByIssues.flat() || []);
      setRawDataAssets(assetsData)


    }
    catch (error) {
      console.error("Error fetching data:", error);
    }
  }

  const updateWidgets = (data) => {
    setWidgets(
      initialWidgets.map(widget => {
        let count = 0;

        if (widget.key === 'totalIssues') {
          count = tabActive === "content" ? data.issueCount || 0 : data.assets?.issueCount || 0;
        } else {
          count = data[widget.key] || 0;
        }

        return { ...widget, count };
      })
    );
  };

  const handleSearch = (value) => {
    setSearchKey(value);
    const lowerVal = value.toLowerCase();
    setRowData(
      (tabActive === "content" ? rawData : rawDataAssets).filter(item =>
        item.path.toLowerCase().includes(lowerVal) ||
        item.issue.toLowerCase().includes(lowerVal) ||
        item.type.toLowerCase().includes(lowerVal) ||
        item.status.toLowerCase().includes(lowerVal)
      )
    );
  }

  const handleFilter = (key, value) => {
    console.log(key, value)
    if (value.toLowerCase() !== "all") {
      if (key === "type") {
        setSearchKey("");
        let filterData = (tabActive === "content" ? rawData : rawDataAssets).filter((item) => {
          return item.type.toLowerCase() === value.toLowerCase()
        })
        setRowData(filterData)
      }
      if (key === "status") {
        setSearchKey("");
        let filterData = rowData.filter((item) => {
          return item.status.toLowerCase() === value.toLowerCase();
        })
        setRowData(filterData)
      }
    }
    else {
      setRowData(tabActive === "content" ? rawData : rawDataAssets)
    }
  }

  console.log("widgets", widgets)

  return (
    <>
      <div class="ch-dashboard__header">
        <div class="ch-dashboard__container">
          <div className="ch-dashboard__header-content">
            <h1 className="ch-dashboard__header-title">Health Dashboard</h1>
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
            <div className="right">
              <div className="ch-dashboard__widget">
                <div className="ch-dashboard__widget-content">
                  <p className="type">Issues By Type</p>
                  <Linear data={rawData} />
                </div>
              </div>
            </div>
          </div>
          <div className="ch-dashboard__content">
            <div className="ch-dashboard__tabs">
              <a onClick={(e) => { e.preventDefault(); setTabActive("content") }} className={tabActive === "content" ? "active" : ""} href="#">Content</a>
              <a onClick={(e) => { e.preventDefault(); setTabActive("assets") }} className={tabActive === "assets" ? "active" : ""} href="#">Assets</a>
            </div>
            <div className="ch-dashboard__content-header">
              <div className="text-wraper">
                <span><svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="#000"><path d="M765-144 526-383q-30 22-65.79 34.5-35.79 12.5-76.18 12.5Q284-336 214-406t-70-170q0-100 70-170t170-70q100 0 170 70t70 170.03q0 40.39-12.5 76.18Q599-464 577-434l239 239-51 51ZM384-408q70 0 119-49t49-119q0-70-49-119t-119-49q-70 0-119 49t-49 119q0 70 49 119t119 49Z"/></svg></span>
                <input value={searchKey} onChange={(e) => handleSearch(e.target.value)} className="form-control text" type="text" placeholder="Search..." />
                {searchKey.length > 0 && <a onClick={() => {
                  setSearchKey("");
                  setRowData(rawData);
                }} className="clear-search">
                  <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="#000"><path d="m338-288-50-50 141-142-141-141 50-50 142 141 141-141 50 50-141 141 141 142-50 50-141-141-142 141Z"/></svg>
                </a>}
              </div>
              <div className="ch-dashboard__content-filters">
                {tabActive === "content" &&
                  <div className="select-warper">
                    <Select selectType={"type"} data={rawData} handleFilter={handleFilter} />
                  </div>
                }
                <div className="select-warper">
                  {/* <select className="form-control select" onChange={(e) => handleFilter("status", e.target.value)}>
                    <option value="all">All status</option>
                    {status?.map((item, index) => (
                      <option key={index} value={item.name}>{item.name}</option>
                    ))}
                  </select> */}
                  <Select selectType={"status"} data={rawData} handleFilter={handleFilter} />
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