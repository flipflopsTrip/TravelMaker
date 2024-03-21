package com.a305.travelmaker.domain.travel.service;

import com.a305.travelmaker.domain.destination.entity.Destination;
import com.a305.travelmaker.domain.destination.repository.DestinationRepository;
import com.a305.travelmaker.domain.destination.service.DestinationService;
import com.a305.travelmaker.domain.travel.dto.Cluster;
import com.a305.travelmaker.domain.travel.dto.Point;
import com.a305.travelmaker.domain.travel.dto.TravelRequest;
import com.a305.travelmaker.domain.travel.dto.TravelResponse;
import com.a305.travelmaker.domain.travel.repository.TravelRepository;
import com.a305.travelmaker.global.common.dto.DestinationDistanceResponse;
import com.a305.travelmaker.global.util.HarversineUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TravelService {

  private final DestinationService destinationService;
  private final TravelRepository travelRepository;
  private final DestinationRepository destinationRepository;
  private final HarversineUtil harversineUtil;

  private List<Point> pointList = new ArrayList<>(); // 장소의 경도, 위도를 담고 있는 리스트
  private List<Cluster> clusters = new ArrayList<>();
  private List<Integer> destinationsIdList = new ArrayList<>(); // 군집내에 속해 있는 ID 리스트

  @Transactional
  public TravelResponse saveTravel(TravelRequest travelRequest) {

    Map<Integer, List<DestinationDistanceResponse>> travelList = new HashMap<>();

    /*
      0. 데이터 세팅
      1. 군집화 실행 (군집 개수 ≤ 여행일 수)
      2. 빅데이터 서버에 군집별로 (센터포인트, R, id리스트) 넘겨서 군집별로 장소 ID 리스트(유저가 선택한 장소 + 빅데이터 기반 추천 장소 리스트) 받기
      3. 카테고리 겹치지 않게 로직 구성 (식당1, 카페1, 관광지1 무조건 들어가게 하고, 다 포함되지 않는 군집에는 근처 반경에서 장소 추가
      4. 모든 장소에 대한 최적 거리 탐색 (모든 출발지에 대한 다익스트라 실행)
      5. 데이터 베이스 저장 후 응답 객체 형식에 맞춰서 데이터 반환
    */

    /*
      0. 데이터 세팅
        0-0. 이전 군집 내용 초기화
        0-1. 장소 id의 경도, 위도를 pointList에 담기
        0-2. 여행일 수 확인
    */
    clusters.clear();
    for (Integer destinationId : travelRequest.getDestinationIdList()) {

      Destination destination = destinationRepository.findById(destinationId).get();
      pointList.add(Point.builder()
          .destinationId(destination.getId())
          .latitude(destination.getLatitude())
          .longitude(destination.getLongitude())
          .build());
    }
    long travelDays = travelRequest.calculateTravelDays();

    System.out.println(pointList);
    System.out.println(travelDays);

    /*
      1. 군집화 실행 (군집 개수 ≤ 여행일 수)
        군집화 과정
        1-1. 초기 중심 무작위 선택
        1-2. 각 장소들의 경도와 위도를 기준으로 중심점과의 비교를 통해 군집화
        1-3. 새로운 중심 개선
        1-4. 2, 3번의 과정을 반복하면서 k개의 군집이 생성되면 종료
     */
    initializeClusters(travelDays);
    System.out.println(clusters);
    boolean centroidsChanged = true;
    while (centroidsChanged) {
      assignPointsToClusters(); // 중심점을 기준으로 각 군집에 할당
      centroidsChanged = updateCentroids(); // 중심점 재계산 - 변경점이 없다면 종료
    }

    for (int i = 0; i < clusters.size(); i++) {

      System.out.println("군집 " + (i + 1) + ": " + clusters.get(i));
    }

    /*
      2. 빅데이터 서버에 군집별로 (센터포인트, R, id리스트) 넘겨서 군집별로 장소 ID 리스트(유저가 선택한 장소 + 빅데이터 기반 추천 장소 리스트) 받기
     */

    /*
      3. 카테고리 겹치지 않게 로직 구성 (식당1, 카페1, 관광지1 무조건 들어가게 하고, 다 포함되지 않는 군집에는 근처 반경에서 장소 추가
     */

    /*
      4. 모든 장소에 대한 최적 거리 탐색 (모든 출발지에 대한 다익스트라 실행)
     */

    /*
      5. 데이터 베이스 저장 후 응답 객체 형식에 맞춰서 데이터 반환
     */
    for (int i = 0; i < travelDays; i++) { // 각 군집별로 장소 ID 확인

      destinationsIdList = new ArrayList<>();
      for (int j = 0; j < clusters.get(i).getPoints().size(); j++) {

        Integer pointId = clusters.get(i).getPoints().get(j).getDestinationId();
        destinationsIdList.add(pointId);
      }

      List<DestinationDistanceResponse> destinationDistanceResponses = destinationService.findDestinationDistance(
          destinationsIdList);
      travelList.put(i + 1, destinationDistanceResponses);
    }

    TravelResponse travelResponse = new TravelResponse(travelList);

    return travelResponse;
  }

  // 초기 중심 무작위 선택 (K-Means ++ Algorithm)
  private void initializeClusters(long travelDays) {

    // 첫 번째 중심점을 장소 리스트 안에서 무작위로 선택
    int index = new Random().nextInt(pointList.size());
    Point firstCentroid = pointList.get(index);
    Cluster firstCluster = new Cluster(firstCentroid);
    clusters.add(firstCluster);

    // 나머지 중심점을 선택
    for (int i = 1; i < travelDays; i++) {

      // 각 데이터 포인트에 대해 가장 먼 중심점의 거리를 계산
      List<Double> distances = new ArrayList<>();
      for (Point point : pointList) {

        double maxDistance = Double.MIN_VALUE;
        for (Cluster cluster : clusters) {

          // 클러스터(군집)의 중심점과 현재점 과의 거리 계산
          double distance = harversineUtil.calculateDistance(cluster.getCentroid(), point);
          if (distance < maxDistance) {
            maxDistance = distance;
          }
        }

        // 클러스터(군집)의 중심점과 가장 먼 점의 거리를 추가
        distances.add(maxDistance);
      }

      // 각 데이터 포인트를 초기 중심점 후보로 선택할 확률을 계산하여 누적 확률 리스트에 저장
      List<Double> probabilities = new ArrayList<>();
      double totalDistance = 0.0;
      for (double distance : distances) {
        totalDistance += distance;
      }
      double cumulativeProbability = 0.0;
      for (double distance : distances) {
        double probability = distance / totalDistance;
        cumulativeProbability += probability;
        probabilities.add(cumulativeProbability);
      }

      // 누적 확률 리스트를 사용하여 새로운 중심점을 선택
      double randomValue = new Random().nextDouble();
      for (int j = 0; j < probabilities.size(); j++) {

        if (randomValue > probabilities.get(j)) {
          Point newCentroid = pointList.get(j);
          Cluster newCluster = new Cluster(newCentroid);
          clusters.add(newCluster);
          break;
        }
      }
    }
  }


  private void assignPointsToClusters() { // 데이터 포인트를 가장 가까운 클러스터에 할당하는 역할

    for (Cluster cluster : clusters) {
      cluster.clearPoints();
    }

    for (Point point : pointList) {

      Cluster nearestCluster = null;
      double minDistance = Double.MAX_VALUE;
      for (Cluster cluster : clusters) {

        double distance = harversineUtil.calculateDistance(cluster.getCentroid(), point);
        if (distance < minDistance) {
          minDistance = distance;
          nearestCluster = cluster;
        }
      }
      nearestCluster.addPoint(point);
    }
  }

  private boolean updateCentroids() { // 중심점이 변경되었는지 확인

    boolean centroidsChanged = false;
    for (Cluster cluster : clusters) {

      Point oldCentroid = cluster.getCentroid();
      Point newCentroid = cluster.calculateCentroid();
      double distance = harversineUtil.calculateDistance(oldCentroid,
          newCentroid); // 이전 중심점과 새 중심점 간의 거리 계산

      if (distance > 0.001) { // 일정한 임계값보다 거리가 크게 변했는지 확인

        centroidsChanged = true;
        cluster.setCentroid(newCentroid);
      }
    }
    return centroidsChanged;
  }
}
