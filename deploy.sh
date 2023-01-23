rm -rf resources/contour-by-example/js;
rm -rf resources/climate-change/js;
npx shadow-cljs release :climate-change;
npx shadow-cljs release :contour-by-example;
vercel;
