import { combineReducers } from "redux"
import { routerReducer as routing } from 'react-router-redux'

import users from "./userReducer"
import jobs from "./jobReducer"

export default combineReducers({
  users,
  jobs,
  routing
})