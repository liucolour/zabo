import React from "react"
import "./Search.css"

export default class Search extends React.Component {
  handleSubmit(){
    console.log("onSubmit click");
  }
  render(){
    return (
      <form class="form-inline" onSubmit={this.handleSubmit.bind(this)}>
        <div class="form-group">
          <label class="sr-only" for="exampleInputName2">Category</label>
          <select class="form-control" name="Category">
            <option value="Rental">Rental</option>
            <option value="Job">Job</option>
            <option value="Trade">Trade</option>
          </select>
        </div>
        <div class="form-group">
          <label class="sr-only" for="exampleInputName2">Category</label>
          <input name="Destination" class="form-control" type="text" placeholder="Destination"/>
        </div>
        <div class="form-group">
          <input type="submit" value="Search"/>
        </div>   
      </form>
    );
  }
}