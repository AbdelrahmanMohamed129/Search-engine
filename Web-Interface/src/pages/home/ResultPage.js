import React from "react";
import classes from "./resultpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ResultedBlock from "../../components/ResultedBlock";
import axios from "axios";

const ResultPage = () => {
  const [results, setResults] = React.useState([]);
  const handleClick = (e) => {
    if (e.key === "Enter") {
      console.log(e.target.value);
      getResults();
    }
  };

  async function getResults() {
    try {
      const response = await axios.get("http://localhost:3001/results");
      setResults(response.data);
      console.log(response);
    } catch (error) {
      console.error(error);
    }
  }

  function renderResults(obj) {
      return (
        <ResultedBlock
          key={obj.id}
          title={obj.title}
          link={obj.link}
          content={obj.content}
        />
      );
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
