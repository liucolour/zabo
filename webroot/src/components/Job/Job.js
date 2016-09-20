import React from "react"
import SearchBar from "./SearchBar.js"
import ContentList from "./ContentList.js"

import { connect } from 'react-redux'

@connect((store) => {
  return {
    jobs: store.jobs.jobs,
  };
})

export default class Rental extends React.Component {
  render() {
    console.log("Got job data");
    return(
      <div class="container-fluid">
        <div class="row">
          <div class="col-xs-3 col-sm-3 col-md-3 col-lg-3"><SearchBar /></div>
          <div class="col-xs-9 col-sm-9 col-md-9 col-lg-9"><ContentList posts={this.props.jobs}/></div>
        </div>
      </div>
    );
  }
}