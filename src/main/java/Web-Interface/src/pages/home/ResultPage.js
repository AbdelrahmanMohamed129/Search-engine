import React, { useEffect } from "react";
import classes from "./resultpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ResultedBlock from "../../components/ResultedBlock";
import ButtonPage from "../../components/ButtonPage";
import axios from "axios";


const ResultPage = () => {
  const [results, setResults] = React.useState([]);
  const [numberOfPages, setNumberOfPages] = React.useState(1)
  const [page, setPage] = React.useState(1)
  const [queryy, setQuery] = React.useState("")

  const handleClick = (e) => {
    if (e.key === "Enter") {
      setQuery(e.target.value)
      getResults(e.target.value);
    }
  };


  async function getResults(query) {

      const response = await axios.get(`http://localhost:8080/search?q=${query}&page=1`).then((response) => {
        
        setResults(response.data.pages);
        var temp = response.data.pages.length
        setNumberOfPages(Math.ceil(temp/10))
        console.log(1)
        console.log(response.data.pages)
      })
     .catch(error => {
      // Handle any errors that occurred during the request
      console.error(error);
    });
  }

  async function getPaginatedResults(page) {

    const response = await axios.get(`http://localhost:8080/search?q=${queryy}&page=${page}`).then((response) => {
      
      setResults(response.data.pages);
      var temp = response.data.pages.length
      setNumberOfPages(Math.ceil(temp/10))
      console.log(2)

      console.log(response.data.pages)
    })
   .catch(error => {
    // Handle any errors that occurred during the request
    console.error(error);
  });
}

  // useEffect on results
  useEffect(() => {
    getPaginatedResults(page)
  }, [page]);

  function renderResults(obj) {
      // map on resylt using genetratedn index
      return <ResultedBlock key={obj.url} title={obj.title} url={obj.url} snippet={obj.snippet} />
  }

  function renderButtons() {
    // map on number of pages and render the ButtonPage component
    var buttons = []
    console.log(numberOfPages)
    for (var i = 1; i <= numberOfPages; i++) {
      buttons.push(<ButtonPage key={i} num={i} setter={setPage} />)
    }
    return buttons

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
        {/* map on buttons using index for pagination*/}
        <div className={classes.pagination}>
          {
            renderButtons()
          }
          
        </div>
    </div>
  );
};

export default ResultPage;
