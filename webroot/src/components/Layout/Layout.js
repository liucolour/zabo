import React, { Component } from 'react';
import Header from '../Header';
// import Feedback from '../Feedback';
import Footer from '../Footer';

export default class Layout extends Component {

  render() {
    const containerStyle = {
      marginTop: "60px"
    };
    return (
      <div>
        <Header />
        <div class="container" style={containerStyle}>
          {this.props.children}
        </div>
        <Footer />
      </div>
    );
  }
}
