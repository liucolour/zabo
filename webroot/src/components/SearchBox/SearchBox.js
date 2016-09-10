import React from "react"
// import "./Search.css"

export default class Search extends React.Component {
  handleSubmit(){
    console.log("onSubmit click");
  }
  render(){
    return (
      <form class="form-inline" onSubmit={this.handleSubmit.bind(this)}>
        <div class="form-group">
          <select class="selectpicker form-control" name="Category">
            <option value="Rental">Rental</option>
            <option value="Job">Job</option>
            <option value="Sale">Sale</option>
            <option value="Car">Sell Car</option>
          </select>
          <input name="Destination" class="form-control" type="text" placeholder="Destination"/>
          <button class="form-control btn btn-info" type="submit">
            <span class="glyphicon glyphicon-search" aria-hidden="true"></span>
          </button>
        </div>   
      </form>
    );
  }
}