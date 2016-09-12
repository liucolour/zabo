import React, { Component } from 'react';
import Header from '../Header';
import Footer from '../Footer';

export default class Layout extends Component {

  render() {
    const containerStyle = {
      marginTop: "50px"
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
