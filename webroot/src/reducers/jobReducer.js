export default function reducer(state={
    jobs: [],
    fetching: false,
    fetched: false,
    error: null,
  }, action) {

    switch (action.type) {
      case "FETCH_JOBS": {
        return {...state, fetching: true}
      }
      case "FETCH_JOBS_REJECTED": {
        return {...state, fetching: false, error: action.payload}
      }
      case "FETCH_JOBS_FULFILLED": {
        return {
          ...state,
          fetching: false,
          fetched: true,
          jobs: action.payload,
        }
      }
      case "ADD_JOB": {
        return {
          ...state,
          jobs: [...state.jobs, action.payload],
        }
      }
      case "UPDATE_JOB": {
        const { id, text } = action.payload
        const newJobs = [...state.jobs]
        const jobToUpdate = newJobs.findIndex(job => job.id === id)
        newJobs[jobToUpdate] = action.payload;

        return {
          ...state,
          jobs: newJobs,
        }
      }
      case "DELETE_JOB": {
        return {
          ...state,
          jobs: state.jobs.filter(job => job.id !== action.payload),
        }
      }
    }

    return state
}
