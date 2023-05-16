import React, { useEffect } from "react";
import classes from "./resultpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ResultedBlock from "../../components/ResultedBlock";
import axios from "axios";

const jsonp = require('jsonp');

const ResultPage = () => {
  const [results, setResults] = React.useState([]);
  const handleClick = (e) => {
    if (e.key === "Enter") {
      getResults(e.target.value);


    }
  };


  async function getResults(query) {

      const response = await axios.get(`http://localhost:8000/search?q=${query}&page=1`).then((response) => {
        console.log(response.data);
        setResults(response.data.pages);
      })
     .catch(error => {
      // Handle any errors that occurred during the request
      console.error(error);
    });
  }

  // useEffect on results
  useEffect(() => {
    console.log(results[1]);
  }, [results]);

  function renderResults(obj) {
      // map on resylt using genetratedn index
      return <ResultedBlock key={obj.url} title={obj.title} url={obj.url} snippet={obj.snippet} />
  }
  return (
    
    <div className={classes.container}>
        <div className={classes.head}>
            <h1>Bingo</h1>
            <div className={classes.search}>
            <input className={classes.searchBar} type="search" onKeyDown={handleClick}/>
            <i className={classes.fa}  >
                    <SearchOutlinedIcon sx={{fontSize:"3.5rem"}}/>
                </i>
            </div>
        </div>
        <div className={classes.border}></div>
        {results?.map(renderResults)}
    </div>
  );
};

export default ResultPage;
