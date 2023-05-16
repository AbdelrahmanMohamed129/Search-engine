import axios from "axios";

const instance  = axios.create({
    baseURL: "https://have-a-dream.onrender.com/",
    headers: {
        Authorization: "Bearer "+ sessionStorage.getItem("tokenValue"),
        ID: sessionStorage.getItem("ID")
    }
})

export default instance