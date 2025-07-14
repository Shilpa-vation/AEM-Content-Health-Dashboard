
import React, { useEffect } from "react";
import Widget from "./widget";
import Navigation from "./navigation";
import Grid from "./grid";
import Linear from "./linear";
import './dashboard.scss';
// import pageIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/page.svg';
// import warningIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/warning.svg';
// import publishedIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/published.svg';
// import unpublishedIcon from '../../../../../../../ui.apps/src/main/content/jcr_root/apps/chd/clientlibs/clientlib-site/resources/images/unpublished.svg';


const initialContentWidgets = [
  { id: 1, name: 'Total Pages', key: 'totalPages', icon: <path d="M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h640q33 0 56.5 23.5T880-720v480q0 33-23.5 56.5T800-160H160Zm0-80h420v-140H160v140Zm500 0h140v-360H660v360ZM160-460h420v-140H160v140Z"/> },
  { id: 2, name: 'Published Pages', key: 'publishedPages', icon: <path d="m424-296 282-282-56-56-226 226-114-114-56 56 170 170Zm56 216q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z"/> },
  { id: 3, name: 'Total Issues', key: 'totalIssues', icon: <path d="m40-120 440-760 440 760H40Zm138-80h604L480-720 178-200Zm302-40q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5 28.5T480-240Zm-40-120h80v-200h-80v200Zm40-100Z"/>},
  { id: 4, name: 'Unpublished Pages', key: 'unpublishedPages', icon: <path d="M819-28 701-146q-48 32-103.5 49T480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-62 17-117.5T146-701L27-820l57-57L876-85l-57 57ZM480-160q45 0 85.5-12t76.5-33L487-360l-63 64-170-170 56-56 114 114 7-8-226-226q-21 36-33 76.5T160-480q0 133 93.5 226.5T480-160Zm335-100-59-59q21-35 32.5-75.5T800-480q0-133-93.5-226.5T480-800q-45 0-85.5 11.5T319-756l-59-59q48-31 103.5-48T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 61-17 116.5T815-260ZM602-474l-56-56 104-104 56 56-104 104Zm-64-64ZM424-424Z"/> }
];

const status = [
  { name: 'Critical', color: 0 },
  { name: 'Warning', color: 0 },
  { name: 'Minor', color: 0 },
]

const types = [
  { name: 'Metadata', color: 0 },
  { name: 'SEO', color: 0 },
  { name: 'Audit', color: 0 },
]

export default function Dashboard() {
  const [tabActive, setTabActive] = React.useState('content');
  const [widgets, setWidgets] = React.useState(initialContentWidgets);
  const [rawData, setRawData] = React.useState([]);
  const [rowData, setRowData] = React.useState([]);
  const [searchKey, setSearchKey] = React.useState("");

  useEffect(() => {
    fetchData();
  }, []);
  const fetchData = async () => {
    try {
      const rawdata = await fetch("http://localhost:4502/bin/content-health-audit");
      const data = await rawdata.json();
      console.log(data);
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


      if (tabActive === 'content') {
        setRowData(contentDataByIssues.flat() || []);
        setRawData(contentDataByIssues.flat() || [])
      }
      else {
        setRowData(data?.assets?.issues || []);
        setRawData(contentDataByIssues.flat() || [])
      }

    }
    catch (error) {
      console.error("Error fetching data:", error);
    }
  }

  const updateWidgets = (data) => {
    setWidgets(
      initialContentWidgets.map(widget => {
        let count = 0;

        if (widget.key === 'totalIssues') {
          count = data.pages?.length || 0;
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
      rawData?.filter(item =>
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
        let filterData = rawData.filter((item) => {
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
      setRowData(rawData)
    }
  }
  console.log("widgets",widgets)

  return (
    <>
      <div class="ch-dashboard__header">
        <div class="ch-dashboard__container">
          <div className="ch-dashboard__header-content">
            <h1 className="ch-dashboard__header-title">Health Dashboard</h1>
            <div className="ch-dashboard__tabs">
              <a onClick={(e) => { e.preventDefault(); setTabActive("content") }} className={tabActive === "content" ? "active" : ""} href="#">Content</a>
              <a onClick={(e) => { e.preventDefault(); setTabActive("assets") }} className={tabActive === "assets" ? "active" : ""} href="#">Assets</a>
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
            <div className="right">
              <div className="ch-dashboard__widget">
                <div className="ch-dashboard__widget-content">
                  <p className="type">Issues By Type</p>
                  <Linear data={rawData} />
                  {/* <ul>
                    <li>
                      <h4>Metadata<span>7</span></h4>
                      <div className="ch-dashboard__widget-icon">Linear1</div>
                    </li>
                    <li>
                      <h4>SEO<span>5</span></h4>
                      <div className="ch-dashboard__widget-icon">Linear2</div>
                    </li>
                    <li>
                      <h4>Audit<span>2</span></h4>
                      <div className="ch-dashboard__widget-icon">Linear3</div>
                    </li>
                  </ul> */}
                </div>
              </div>
            </div>
          </div>
          <div className="ch-dashboard__content">
            <div className="ch-dashboard__content-header">
              <div className="text-wraper">
                <input value={searchKey} onChange={(e) => handleSearch(e.target.value)} className="form-control text" type="text" placeholder="Search..." />
                {searchKey.length > 0 && <a onClick={() => {
                  setSearchKey("");
                  setRowData(rawData);
                }} className="clear-search">
                  x
                </a>}
              </div>
              <div className="ch-dashboard__content-filters">
                <div className="select-warper">
                  <select className="form-control select" onChange={(e) => handleFilter("type", e.target.value)}>
                    <option value="all">All</option>
                    {types?.map((type, index) => (
                      <option key={index} value={type.name}>{type.name}</option>
                    ))}
                  </select>
                </div>
                <div className="select-warper">
                  <select className="form-control select" onChange={(e) => handleFilter("status", e.target.value)}>
                    <option value="all">All</option>
                    {status?.map((item, index) => (
                      <option key={index} value={item.name}>{item.name}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
            <div className="ch-dashboard__content-item">
              <Grid rowData={rowData} />
            </div>
          </div>
        </div>
      </div>
    </>
  );
}