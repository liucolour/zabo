import React from 'react';
import { IndexLink, Link } from "react-router";
import Navigation from '../Navigation';
import logoUrl from './logo-small.png';

export default function Header() {
  return (
    <div class="container">
        <Navigation />
    </div>
  );
}