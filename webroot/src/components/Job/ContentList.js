import React from "react"
import Posts from "./posts"

export default class ContentList extends React.Component {
  render() {
    const {hits} = this.props.posts;
    if (hits) {
      const PostsComponents = hits.hits.map((post) => {
        console.log("id: " + post._id);
        return <Posts key={post._id} {...post._source} />
      });

      return(
        <div class="container">
          <div class="row">
            <div class="col-md-12">
              <p>Job work</p>
              <hr/>
            </div>
          </div>
          {PostsComponents}
        </div>
      );
    } else {
      return (
        <div class="container">
          <div class="row">
            <div class="col-md-12">
              <p>Failed to retrieve posts</p>
              <hr/>
            </div>
          </div>
        </div>
      );
    }
  }
}