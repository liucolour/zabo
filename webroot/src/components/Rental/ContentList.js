import React from "react"

export default class ContentList extends React.Component {
  render() {
    return(
      <div class="container">
        <div class="row">
          <div class="col-md-12">
            <p>Page Heading</p>
            <hr/>
          </div>
        </div>
        <div class="row">
          <div class="col-md-8">
            <h3>Rental One</h3>
            <h4>Subheading</h4>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Laudantium veniam exercitationem expedita laborum at voluptate. Labore, voluptates totam at aut nemo deserunt rem magni pariatur quos perspiciatis atque eveniet unde.</p>
            <a class="btn btn-primary" href="#">View Project <span class="glyphicon glyphicon-chevron-right"></span></a>
          </div>
        </div>
        <div class="row">
          <div class="col-md-8">
            <h3>Rental Two</h3>
            <h4>Subheading</h4>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Laudantium veniam exercitationem expedita laborum at voluptate. Labore, voluptates totam at aut nemo deserunt rem magni pariatur quos perspiciatis atque eveniet unde.</p>
            <a class="btn btn-primary" href="#">View Project <span class="glyphicon glyphicon-chevron-right"></span></a>
          </div>
        </div>
      </div>
    );
  }
}