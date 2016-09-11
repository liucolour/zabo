import React from "react"
import { Link, IndexLink } from "react-router"
import "./Navigation.css"

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

  handleSubmit(){
    console.log("onSubmit click");
  }

  render() {
    const { collapsed } = this.state;
    const navClass = collapsed ? "collapse" : "";
    return (
      <nav class="navbar navbar-default navbar-fixed-top" role="navigation">
          <div class="container">
              <div class="navbar-header">
                  <button type="button" class="navbar-toggle" onClick={this.toggleCollapse.bind(this)}>
                      <span class="sr-only">Toggle navigation</span>
                      <span class="icon-bar"></span>
                      <span class="icon-bar"></span>
                      <span class="icon-bar"></span>
                  </button>
                  <IndexLink class="navbar-brand" to="/">Home</IndexLink>      
              </div>
              <div class={"navbar-collapse " + navClass} id="bs-example-navbar-collapse-1">            
                <ul class="nav navbar-nav navbar-right">    
                    <li>
                      <Link to="postCreation">Create new Post</Link>
                    </li>
                    <li>
                      <Link to="login">Log in</Link>
                    </li>
                    <li>
                      <Link to="signup">Sign up</Link>                      
                    </li>                      
                </ul>
                <form class="navbar-form" id="search" role="Search" onSubmit={this.handleSubmit.bind(this)}>
                  <div class="form-group">
                    <select class="selectpicker form-control" name="Category">
                      <option selected disabled>Category</option>
                      <option value="Rental">Rental</option>
                      <option value="Job">Job</option>
                      <option value="Sale">Sale</option>
                      <option value="Car">Sell Car</option>
                    </select>
                    <input name="Destination" class="form-control" type="text" placeholder="Place"/>
                    <button class="btn btn-info" type="submit">
                      <span class="glyphicon glyphicon-search" aria-hidden="true"></span>
                    </button>
                  </div>   
                </form> 
              </div>
          </div>
      </nav>
  );
    }
}