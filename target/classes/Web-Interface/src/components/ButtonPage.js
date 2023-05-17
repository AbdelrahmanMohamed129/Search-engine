import React from "react";
import classes from "../pages/home/resultpage.module.css"



const ButtonPage = (props) => {
    function handleClick() {
        props.setter(props.num)
    }
    return (
    
        <div className={classes.button} onClick={handleClick}>
           {props.num}
           
        </div>
      );
};

export default ButtonPage;

