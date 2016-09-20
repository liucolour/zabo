import React from "react"
import ReactDOM from "react-dom"
import { Provider } from "react-redux"
import { applyMiddleware, createStore } from "redux"
import { Router, Route, IndexRoute, browserHistory } from "react-router"
import { syncHistoryWithStore } from 'react-router-redux'

import logger from "redux-logger"
import thunk from "redux-thunk"
import promise from "redux-promise-middleware"

import Layout from "./components/Layout"
import reducer from "./reducers"

import Content from "./components/Content"
import Rental from "./components/Rental"
import Job from "./components/Job"
import Login from "./components/Login"
import Signup from "./components/Signup"

const middleware = applyMiddleware(promise(), thunk, logger())

const store = createStore(reducer, middleware)

const app = document.getElementById('app')

// Create an enhanced history that syncs navigation events with the store
const history = syncHistoryWithStore(browserHistory, store)

ReactDOM.render(
  <Provider store={store}>
    <Router history={history}>
      <Route path="/" component={Layout}>
        <IndexRoute component={Content}></IndexRoute>
        <Route path="login" component={Login}></Route>
        <Route path="signup" component={Signup}></Route>

        <Route path="rental" component={Rental}></Route>
        <Route path="job" component={Job}></Route>
      </Route>
    </Router>
  </Provider>, app);