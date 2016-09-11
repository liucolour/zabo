import React from "react"
import ReactDOM from "react-dom"
import { Provider } from "react-redux"
import { applyMiddleware, createStore } from "redux"
import { Router, Route, IndexRoute, hashHistory } from "react-router"

import logger from "redux-logger"
import thunk from "redux-thunk"
import promise from "redux-promise-middleware"

import Layout from "./components/Layout"
import reducer from "./reducers"

import Contents from "./components/Contents"
import Login from "./components/Login"
import Signup from "./components/Signup"

const middleware = applyMiddleware(promise(), thunk, logger())

const store = createStore(reducer, middleware)

const app = document.getElementById('app')


ReactDOM.render(<Provider store={store}>
  <Router history={hashHistory}>
    <Route path="/" component={Layout}>
      <IndexRoute component={Contents}></IndexRoute>
      <Route path="login" component={Login}></Route>
      <Route path="signup" component={Signup}></Route>
    </Route>
  </Router>
</Provider>, app);