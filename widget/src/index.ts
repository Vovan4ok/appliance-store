// For React apps that want the components instead of the <script> tag (styles: import widgetStyles)
export { RecommendationsWidget, type RecommendationsWidgetProps } from './RecommendationsWidget';
export { RecommendationList, type RecommendationListProps } from './components/RecommendationList';
export { ProductCard, type ProductCardProps } from './components/ProductCard';
export { useRecommendations } from './useRecommendations';
export { fetchRecommendations, ApiError } from './api';
export type { Recommendation, RecommendationsState, ReasonCode } from './types';
export { default as widgetStyles } from './styles.css?inline';
