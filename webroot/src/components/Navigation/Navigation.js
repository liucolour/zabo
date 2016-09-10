import React from "react"
import { Link, IndexLink } from "react-router"
import "./Navigation.css"
import Search from "../SearchBox"

export default class Navigation extends React.Component {
  constructor() {
    super()
    this.state = {
      collapsed: true,
    };
  }

  toggleCollapse() {
    const collapsed = !this.state.collapsed;
    this.setState({collapsed});
  }

  render() {
    const { collapsed } = this.state;
    const navClass = collapsed ? "collapse" : "";
    return (
      <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
          <div class="container">
              <div class="navbar-left">
                  <button type="button" class="navbar-toggle" onClick={this.toggleCollapse.bind(this)}>
                      <span class="sr-only">Toggle navigation</span>
                      <span class="icon-bar"></span>
                      <span class="icon-bar"></span>
                      <span class="icon-bar"></span>
                  </button>
                  <IndexLink class="navbar-brand" to="/">Home</IndexLink>      
              </div>
              <div class={"navbar-right navbar-collapse " + navClass} id="bs-example-navbar-collapse-1">
                  <ul class="nav navbar-nav">
                      <li class="searchBox">
                        <SearchBox />        
                      </li>      
                      <li>
                        <Link to="postCreation"><button class="btn btn-success">Create new Post</button></Link>
                      </li>
                      <li>
                        <Link to="login">Log in</Link>
                      </li>
                      <li>
                        <Link to="signup">Sign up</Link>                      
                      </li>                      
                  </ul>
              </div>
          </div>
      </nav>
  );
    }
}