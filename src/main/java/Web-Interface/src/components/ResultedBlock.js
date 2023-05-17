import React from "react";
import classes from "./resultedblock.module.css";


const ResultedBlock = (props) => {
  return (
    <div className={classes.container}>
    <a href={props.url} className={classes.title}>{props.title}</a>
    <a href={props.url} className={classes.link}>{props.url}</a>
    <div className={classes.content} dangerouslySetInnerHTML={{__html: props.snippet}}></div>
    </div>
  );
};

export default ResultedBlock;
