import React from "react";

export default class Posts extends React.Component {

  render() {
    const { title, description, location, is_provider, modified_time } = this.props;
    const role_lable = is_provider? "label-warning": "lable-danger";
    const role = is_provider? "provider": "demander";
    var d = new Date(modified_time);
    var date = d.getUTCDate();

    var monthNames = ["January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"];

    var month = d.getUTCMonth();
    var ts = date + " " + monthNames[month];
    return (
      <div class="row">
        <div class="col-md-8">
          <h4>{title}</h4>
          <span class="label label-default">{ts}</span>
          <span class={"label " + role_lable}>{role}</span> 
          <span class="label label-info">{location.city}</span>
          <p>{description}</p>
        </div>
      </div>
    );
  }
}