import React, { Component } from 'react'
import Navigation from '../Navigation'
import Footer from '../Footer'
import { Router } from 'react-router'

export default class Layout extends Component {
  constructor(props, context) {
      super(props, context);
  }
  handleSearch(category, place){
    console.log("handleSearch in layout");
    console.log("Category : " + category);
    console.log("Destination : " + place);
    this.context.router.push(category.toLowerCase(), {objectId: 'asdf'});
  }

  render() {
    const containerStyle = {
      marginTop: "60px"
    };
    return (
      <div>
        <Navigation handleSearch={this.handleSearch.bind(this)}/>
        <div class="container" style={containerStyle}>
          {this.props.children}
        </div>
        <Footer />
      </div>
    );
  }
}

Layout.contextTypes = {
  router: React.PropTypes.object.isRequired,
};
