import React from "react";
import classes from "./resultpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ResultedBlock from "../../components/ResultedBlock";

const ResultPage = () => {
  const handleClick = (e) => {
    if (e.key === "Enter") {
      console.log(e.target.value);
      
    }
  };
  return (
    
    <div className={classes.container}>
        <div className={classes.head}>
            <h1>Google</h1>
            <div className={classes.search}>
            <input className={classes.searchBar} type="search" onKeyDown={handleClick}/>
            <i className={classes.fa}  >
                    <SearchOutlinedIcon sx={{fontSize:"3.5rem"}}/>
                </i>
            </div>
        </div>
        <div className={classes.border}></div>
        <ResultedBlock/>
    </div>
  );
};

export default ResultPage;
