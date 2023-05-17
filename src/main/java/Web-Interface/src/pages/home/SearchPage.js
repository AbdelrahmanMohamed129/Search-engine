import React from "react";
import classes from "./searchpage.module.css";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import { useNavigate } from "react-router-dom";
import axios from "axios";


const SearchPage = () => {
  const [found, setFound] = React.useState(false)
  
  const navigate = useNavigate();
  const handleClick = (e) => {
    if (e.key === "Enter") {
      getResults(e.target.value)

    }
  };

  async function getResults(query) {
    setFound(false)
      // get current time 
      var d1 = new Date();
      var n1 = d1.getTime();
      const response = await axios.get(`http://localhost:8000/search?q=${query}&page=1`).then((response) => {
        
        if(response.data === "Nothing was found!! Search one more time ... NOOB =) ") {
            alert(response.data)
        }
        else if(response.data === "Please enter a valid search query!"){
          setFound(false)
          alert(response.data)
        }
        else{
          sessionStorage.setItem("query",query)
          navigate("/result");
        }
      })
     .catch(error => {
      // Handle any errors that occurred during the request
      console.error(error);
    });
  }
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
