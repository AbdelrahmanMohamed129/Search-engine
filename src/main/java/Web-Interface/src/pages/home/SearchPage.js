import React from "react";
import classes from "./searchpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import { useNavigate } from "react-router-dom";


const SearchPage = () => {
  const navigate = useNavigate();
  const handleClick = (e) => {
    if (e.key === "Enter") {
      console.log(e.target.value);
      navigate("/result");
    }
  };
  return (
    
    <div className={classes.container}>
    <h1 className={classes.heading}>Bingo</h1>
    <div className={classes.search}>
      <input className={classes.searchBar} type="search" onKeyDown={handleClick}/>
      <i className={classes.fa}  >
            <SearchOutlinedIcon sx={{fontSize:"3.5rem"}}/>
        </i>
    </div>
    </div>
  );
};

export default SearchPage;
