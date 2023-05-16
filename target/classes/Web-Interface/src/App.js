import "./App.css";
import SearchPage from "./pages/home/SearchPage";
import ResultPage from "./pages/home/ResultPage";
import { Routes, Route} from "react-router-dom";

function App() {
  return (
      <Routes>
        <Route path="/" element={<SearchPage />} />
        <Route path="/result" element={<ResultPage />} />
      </Routes>
  );
}

export default App;
