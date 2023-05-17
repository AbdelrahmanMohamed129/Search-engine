import "./App.css";
import SearchPage from "./pages/home/SearchPage";
import ResultPage from "./pages/home/ResultPage";
import { Routes, Route} from "react-router-dom";

function App() {
  return (
    <div className="App">
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/result" element={<ResultPage />} />
        </Routes>
      </div>
  );
}

export default App;
