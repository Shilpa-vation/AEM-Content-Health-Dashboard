
import React, { useEffect } from "react";
import Widget from "./widget";
import Navigation from "./navigation";
import Grid from "./grid";
import Linear from "./linear";
import './dashboard.scss';
// import pageIcon from './page.svg';
// import warningIcon from './warning.svg';
// import publishedIcon from './published.svg';
// import unpublishedIcon from './unpublished.svg';


const initialContentWidgets = [
  { name: 'Total Pages', count: 0, icon: "pageIcon" },
  { name: 'Published Pages', count: 0, icon: "publishedIcon" },
  { name: 'Total Issues', count: 0, icon: "warningIcon" },
  { name: 'Unpublished Pages', count: 0, icon: "unpublishedIcon" }
]

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
    setWidgets([
      { name: 'Total Pages', count: data.totalPages || 0, icon: "pageIcon" },
      { name: 'Published Pages', count: data.publishedPages || 0, icon: "publishedIcon" },
      { name: 'Total Issues', count: data.pages.length, icon: "warningIcon" },
      { name: 'Unpublished Pages', count: data.unpublishedPages || 0, icon: "unpublishedIcon" }
    ])
  }

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
                <Widget key={index} name={widget.name} count={widget.count} />
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