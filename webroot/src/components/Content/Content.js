import React from "react"
// import {Layout, Fixed, Flex} from "react-layout-pane"
import SearchBar from "./SearchBar.js"
import ContentList from "./ContentList.js"

export default class Content extends React.Component {
  render() {
    return(
      <div class="container-fluid">
        <div class="row">
          <div class="col-xs-3 col-sm-3 col-md-3 col-lg-3"><SearchBar /></div>
          <div class="col-xs-9 col-sm-9 col-md-9 col-lg-9"><ContentList /></div>
        </div>
      </div>
    );
  }
}