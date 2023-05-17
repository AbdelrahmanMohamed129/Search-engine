import React, { useEffect } from "react";
import classes from "./resultpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ResultedBlock from "../../components/ResultedBlock";
import ButtonPage from "../../components/ButtonPage";
import axios from "axios";
import AliceCarousel from 'react-alice-carousel';
import 'react-alice-carousel/lib/alice-carousel.css';
import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew';
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';


const ResultPage = () => {
  const [results, setResults] = React.useState([]);
  const [numberOfPages, setNumberOfPages] = React.useState(1)
  const [page, setPage] = React.useState(1)
  const [queryy, setQuery] = React.useState("")
  const [found, setFound] = React.useState(true)
  const [suggested, setSuggested] = React.useState([])
  const [time,setTime] =React.useState(0)
  const [btns,setBtns] = React.useState([])
  const handleClick = (e) => {
    if (e.key === "Enter") {
      setQuery(e.target.value)
      getResults(e.target.value);
    }
  };

  function handleChange() {
    setQuery(document.getElementById("search").value)
    getSuggestion(document.getElementById("search").value)
    console.log(document.getElementById("search").value)
  }

  async function getResults(query) {
    setFound(true)
      // get current time 
      var d1 = new Date();
      var n1 = d1.getTime();
      const response = await axios.get(`http://localhost:8000/search?q=${query}&page=1`).then((response) => {
        
        if(response.data === "Nothing was found!! Search one more time ... NOOB =) ") {
            setFound(false)
            alert(response.data)
        }
        else if(response.data === "Please enter a valid search query!"){
            setFound(false)
            alert(response.data)
        }
        setResults(response.data.pages);
        // var temp = response.data.pages?.length
        setNumberOfPages(response.data.pagination.pages_count)
        var d2 = new Date();
        var n2= d2.getTime();
        setTime(((n2-n1)*0.001).toFixed(4))
      })
     .catch(error => {
      // Handle any errors that occurred during the request
      console.error(error);
    });
  }

  async function getPaginatedResults(page) {
    var d1 = new Date();
    var n1 = d1.getTime();
    const response = await axios.get(`http://localhost:8000/search?q=${queryy}&page=${page}`).then((response) => {
      
      setResults(response.data.pages);
      var temp = response.data.pages?.length

      var d2 = new Date();
      var n2= d2.getTime();
      setTime(((n2-n1)*0.001).toFixed(4))
    })
   .catch(error => {
    // Handle any errors that occurred during the request
    console.error(error);
  });
}


async function getSuggestion(query) {
    const response = await axios.get(`http://localhost:8000/suggest?q=${query}`).then((response) => {
      setSuggested(response.data.suggestion);

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

  useEffect(() => {
    console.log(suggested)
  }, [suggested]);

  useEffect(() => {
    //get query from session storage
    setQuery(sessionStorage.getItem("query"))
    getResults(sessionStorage.getItem("query"))
  }, []);

  function renderResults(obj) {
      // map on resylt using genetratedn index
      return <ResultedBlock key={obj.url} title={obj.title} url={obj.url} snippet={obj.snippet}  />
  }

  function renderButtons() {
    // map on number of pages and render the ButtonPage component
    // var buttons = []
    // for (var i = 1; i <= numberOfPages; i++) {
    //   buttons.push(<ButtonPage key={i} num={i} setter={setPage} active={page===i}/>)
    // }
    // return buttons

    const buttonsList = []
    for (var i = 1; i <= numberOfPages; i++) {
      // put button data in an array
      const button = {
        key: i,
        num: i,
        setter: setPage,
        active: page===i
        }
      buttonsList.push(button)
      }

      setBtns(buttonsList)
      } 

  useEffect(() => {
    renderButtons()
  }, [numberOfPages]);
  

  return ( 
    
    <div className={classes.container}>

        <div className={classes.head}>
            <h1>Bingo</h1> 
            <div className={classes.search}>
            <input className={classes.searchBar} list="searchlist" id="search" type="search" onKeyDown={handleClick} onChange={handleChange} value={queryy} />
            <datalist id="searchlist" className={classes.suggestion} >
                  {suggested.map((sug) => {return <option value={sug} />})}
        </datalist>

            <i className={classes.fa}  >
                    <SearchOutlinedIcon sx={{fontSize:"3.5rem"}}/>
                </i>
            </div>  
        </div>
            {!found?null:
              <div className={classes.time}>({time} seconds)</div>}
        <div className={classes.border}></div>
        {results?.map(renderResults)}
        {/* map on buttons using index for pagination*/}
        <div className={classes.pagination}>
          {found?
          (
            <AliceCarousel
            className={classes.carousel}
            mouseTracking items={
              btns.map((btn,index) => {
                return <ButtonPage key={btn.key} num={btn.num} setter={btn.setter} active={page===index+1}/>
              })   
            } 
            responsive={{
              0: {
                  items: 1,
                  itemsFit:'fill'
              },
              800: {
                items: 6,
                itemsFit: 'fill',
            },
              1200: {
                  items: 10,
                  itemsFit: 'fill',
              }
            }}
            renderDotsItem={(e) => {return <div className={e.isActive?classes.carouselIndexBtnActive:classes.carouselIndexBtn}></div>}} 
            renderPrevButton={(e)=>{return <ArrowBackIosNewIcon style={{fontSize:"20"}} className={e.isDisabled?classes.carouseLBtnDis:classes.carouseLBtn}/>}}
            renderNextButton={(e)=>{return <ArrowForwardIosIcon style={{fontSize:"20"}} className={e.isDisabled?classes.carouselRtnDis:classes.carouselRBtn}/>}}
          />
          ):
            <h1 className={classes.notFound}>Not Found!</h1>
          }
          
        </div>
    </div>
  );
};

export default ResultPage;
