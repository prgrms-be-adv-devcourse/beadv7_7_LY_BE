# 공식 ES 이미지에는 analysis-common만 들어있고 한국어 형태소 분석기(Nori)는 없다.
# 기본 분석기는 조사를 분리하지 못해 "김광석의 노래"를 색인하면 "김광석"으로 검색해도 안 걸린다.
FROM docker.elastic.co/elasticsearch/elasticsearch:9.4.2

RUN elasticsearch-plugin install --batch analysis-nori
