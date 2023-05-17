import React from "react";
import classes from "../pages/home/resultpage.module.css"



const ButtonPage = (props) => {

    const [active,setActive] = React.useState(false)
    function handleClick() {
        setActive(true)
        props.setter(props.num)
        // make the screen moves to the top
        window.scrollTo(0, 0)
    }
    return (
        
        // conditional className depends on active button   
        <div className={props.active ? classes.active : classes.button} onClick={handleClick}>
           {props.num}
           
        </div>
      );
};

export default ButtonPage;

