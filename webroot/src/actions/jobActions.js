import axios from "axios";

export function fetchJobs() {
  return function(dispatch) {
    axios.get("http://192.168.56.101:9200/post_index/job/_search?pretty")
      .then((response) => {
        dispatch({type: "FETCH_JOBS_FULFILLED", payload: response.data})
      })
      .catch((err) => {
        dispatch({type: "FETCH_JOBS_REJECTED", payload: err})
      })
  }
}