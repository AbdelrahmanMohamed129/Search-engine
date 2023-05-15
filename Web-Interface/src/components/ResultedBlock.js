import React from "react";
import classes from "./resultedblock.module.css";


const ResultedBlock = (props) => {
  return (
    <div className={classes.container}>
    <a className={classes.title}>{props.title}</a>
    <a href={props.link} className={classes.link}>{props.link}</a>
    <div className={classes.content} dangerouslySetInnerHTML={{__html: props.content}}></div>
    </div>
  );
};

export default ResultedBlock;
